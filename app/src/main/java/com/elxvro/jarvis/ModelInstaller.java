package com.elxvro.jarvis;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Installs the offline Turkish model bundled in the APK. The historical
 * vosk-model-small-tr-0.3 has the Vosk v1 layout (final.mdl + mfcc.conf at
 * model root). Do not require v2 am/ and conf/ directories for this model.
 */
public final class ModelInstaller {
    private ModelInstaller() {}

    public interface Progress { void update(String message); }

    private static final String MODEL_NAME = "vosk-model-small-tr-0.3";
    private static final String ASSET = "models/" + MODEL_NAME + ".zip";
    private static final long MAX_EXTRACTED_BYTES = 450L * 1024L * 1024L;
    private static final int MAX_ENTRIES = 3000;

    public static File modelDir(Context context) {
        return new File(context.getFilesDir(), "models/" + MODEL_NAME);
    }

    public static boolean isValidModel(File root) {
        if (!root.isDirectory()) return false;
        // Vosk native API accepts two supported layouts.
        boolean legacy = new File(root, "final.mdl").isFile()
                && new File(root, "mfcc.conf").isFile()
                && new File(root, "HCLr.fst").isFile()
                && new File(root, "Gr.fst").isFile();
        boolean modern = new File(root, "am/final.mdl").isFile()
                && new File(root, "conf/model.conf").isFile()
                && (new File(root, "graph/HCLG.fst").isFile()
                    || (new File(root, "graph/HCLr.fst").isFile()
                        && new File(root, "graph/Gr.fst").isFile()));
        return legacy || modern;
    }

    public static boolean installed(Context context) {
        return isValidModel(modelDir(context));
    }

    /**
     * Called on a background worker thread; never on the main/UI thread.
     * No internet permission or separate model download is required.
     */
    public static void installBundled(Context context, Progress progress) throws Exception {
        if (installed(context)) {
            progress.update("Türkçe tanıma modeli hazır.");
            return;
        }
        File modelsBase = new File(context.getFilesDir(), "models");
        File temp = new File(context.getFilesDir(), "models-installing");
        deleteRecursively(temp);
        if (!temp.mkdirs()) throw new IllegalStateException("Model için alan oluşturulamadı");
        try {
            progress.update("APK içindeki Türkçe model kuruluyor...");
            try (InputStream input = new BufferedInputStream(context.getAssets().open(ASSET))) {
                unpack(input, temp);
            }
            File unpacked = new File(temp, MODEL_NAME);
            if (!isValidModel(unpacked)) {
                throw new IllegalStateException("Kurulan modelin dosya yapısı beklenenden farklı");
            }
            progress.update("Türkçe model doğrulandı, tamamlanıyor...");
            if (!modelsBase.exists() && !modelsBase.mkdirs()) {
                throw new IllegalStateException("Model klasörü oluşturulamadı");
            }
            File destination = modelDir(context);
            deleteRecursively(destination);
            if (!unpacked.renameTo(destination)) {
                throw new IllegalStateException("Model dosyaları taşınamadı");
            }
            if (!installed(context)) {
                throw new IllegalStateException("Kurulu model denetlenemedi");
            }
            progress.update("Model hazır. Mia'yı dinlemeye başlatabilirsin.");
        } finally {
            deleteRecursively(temp);
        }
    }

    private static void unpack(InputStream input, File temp) throws Exception {
        String safeRoot = temp.getCanonicalPath() + File.separator;
        long extracted = 0;
        int count = 0;
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            byte[] data = new byte[32768];
            while ((entry = zip.getNextEntry()) != null) {
                if (++count > MAX_ENTRIES) throw new IllegalStateException("Model arşivi çok fazla dosya içeriyor");
                File target = new File(temp, entry.getName());
                String canonical = target.getCanonicalPath();
                if (!canonical.startsWith(safeRoot)) {
                    throw new SecurityException("Geçersiz model yolu");
                }
                if (entry.isDirectory()) {
                    if (!target.isDirectory() && !target.mkdirs()) {
                        throw new IllegalStateException("Klasör oluşturulamadı");
                    }
                } else {
                    File parent = target.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IllegalStateException("Model klasörü oluşturulamadı");
                    }
                    try (BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(target))) {
                        int length;
                        while ((length = zip.read(data)) != -1) {
                            if (length == 0) continue;
                            extracted += length;
                            if (extracted > MAX_EXTRACTED_BYTES) {
                                throw new IllegalStateException("Model boyutu güvenlik sınırını aştı");
                            }
                            output.write(data, 0, length);
                        }
                    }
                }
                zip.closeEntry();
            }
        }
        if (count == 0) throw new IllegalStateException("Model arşivi boş");
    }

    private static void deleteRecursively(File file) throws Exception {
        if (!file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        if (!file.delete() && file.exists()) {
            throw new IllegalStateException("Geçici model dosyası temizlenemedi");
        }
    }
}
