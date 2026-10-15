package com.elxvro.jarvis;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Downloads the Apache-2.0 licensed Vosk Turkish mobile model directly from the
 * published model host. Call from a worker thread, never on the UI thread.
 */
public final class ModelInstaller {
    private ModelInstaller() {}
    private static final String URL_STRING =
            "https://alphacephei.com/vosk/models/vosk-model-small-tr-0.3.zip";
    private static final String MODEL_NAME = "vosk-model-small-tr-0.3";
    private static final long MAX_EXTRACTED_BYTES = 450L * 1024L * 1024L;

    public interface Progress { void update(String message); }

    public static File modelDir(Context context) {
        return new File(context.getFilesDir(), "models/" + MODEL_NAME);
    }

    public static boolean installed(Context context) {
        File root = modelDir(context);
        return new File(root, "am/final.mdl").isFile()
                && new File(root, "conf/model.conf").isFile();
    }

    public static void downloadAndInstall(Context context, Progress progress) throws Exception {
        if (installed(context)) {
            progress.update("Türkçe model zaten kurulu.");
            return;
        }
        File zipFile = new File(context.getCacheDir(), MODEL_NAME + ".partial.zip");
        File modelsBase = new File(context.getFilesDir(), "models");
        File temp = new File(context.getFilesDir(), "models-installing");
        deleteRecursively(temp);
        if (!temp.mkdirs()) throw new IllegalStateException("Geçici klasör oluşturulamadı");
        HttpURLConnection connection = null;
        try {
            progress.update("Türkçe ses modeli indiriliyor (~35 MB)...");
            connection = (HttpURLConnection) new URL(URL_STRING).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setInstanceFollowRedirects(true);
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("Model indirilemedi, HTTP " + connection.getResponseCode());
            }
            try (InputStream in = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream out = new FileOutputStream(zipFile)) {
                byte[] block = new byte[32768];
                int n;
                long received = 0L;
                while ((n = in.read(block)) >= 0) {
                    if (n == 0) continue;
                    received += n;
                    if (received > 100L * 1024L * 1024L) {
                        throw new IllegalStateException("Model ZIP sınırı aşıldı");
                    }
                    out.write(block, 0, n);
                }
            }
            progress.update("Model ayıklanıyor...");
            String safeRoot = temp.getCanonicalPath() + File.separator;
            long extracted = 0;
            try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(
                    new java.io.FileInputStream(zipFile)))) {
                ZipEntry entry;
                byte[] buf = new byte[32768];
                while ((entry = zip.getNextEntry()) != null) {
                    File target = new File(temp, entry.getName());
                    String canonical = target.getCanonicalPath();
                    if (!canonical.startsWith(safeRoot)) {
                        throw new SecurityException("Güvensiz ZIP yolu");
                    }
                    if (entry.isDirectory()) {
                        if (!target.isDirectory() && !target.mkdirs()) throw new IllegalStateException("Klasör oluşturulamadı");
                    } else {
                        File parent = target.getParentFile();
                        if (!parent.isDirectory() && !parent.mkdirs()) throw new IllegalStateException("Dizin oluşturulamadı");
                        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
                            int n;
                            while ((n = zip.read(buf)) > 0) {
                                extracted += n;
                                if (extracted > MAX_EXTRACTED_BYTES) throw new IllegalStateException("Model çok büyük");
                                out.write(buf, 0, n);
                            }
                        }
                    }
                    zip.closeEntry();
                }
            }
            File downloadedModel = new File(temp, MODEL_NAME);
            if (!new File(downloadedModel, "am/final.mdl").isFile() ||
                !new File(downloadedModel, "conf/model.conf").isFile()) {
                throw new IllegalStateException("İndirilen model geçersiz");
            }
            if (!modelsBase.isDirectory() && !modelsBase.mkdirs()) {
                throw new IllegalStateException("Model klasörü oluşturulamadı");
            }
            File finalModel = modelDir(context);
            deleteRecursively(finalModel);
            if (!downloadedModel.renameTo(finalModel)) {
                throw new IllegalStateException("Model taşınamadı");
            }
            progress.update("Model hazır. Dinlemeyi başlatabilirsin.");
        } finally {
            if (connection != null) connection.disconnect();
            if (zipFile.exists()) zipFile.delete();
            deleteRecursively(temp);
        }
    }

    private static void deleteRecursively(File f) {
        if (!f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File child : children) deleteRecursively(child);
        }
        if (!f.delete() && f.exists()) throw new IllegalStateException("Dosya temizlenemedi");
    }
}
