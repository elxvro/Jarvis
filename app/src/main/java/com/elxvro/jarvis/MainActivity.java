package com.elxvro.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.provider.OpenableColumns;
import android.database.Cursor;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Accessible, large-touch-target UI; fully usable with no voice input. */
public final class MainActivity extends Activity {
    private static final int REQ_MIC = 502;
    private static final int REQ_GGUF = 609;
    private static final int WHITE = 0xffedf2fa;
    private static final int MUTED = 0xffa6b7d0;
    private static final int ACCENT = 0xff5ce3c0;
    private final ExecutorService workers = Executors.newSingleThreadExecutor();
    private TextView status, voiceStatus;
    private EditText typed;
    private VoiceSpeaker voice;
    private Button downloadButton, listenButton;
    private EditText aiInput;
    private TextView aiStatus, aiOutput;
    private Button aiSend;
    private LocalAiBridge localAi;
    private final StringBuilder generated = new StringBuilder();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xff0d1527);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(30), dp(20), dp(30));
        scroll.addView(root);

        TextView brand = text(root, "ELXVRO", 19, ACCENT);
        brand.setTypeface(null, Typeface.BOLD);
        TextView heading = text(root, "MIA", 46, WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        text(root, "v0.3.0 · Yerel yapay zekâ · Ücretsiz · İnternetsiz sohbet", 14, MUTED);
        gap(root, 18);
        TextView intro = text(root, "Sadece “Mia” demen yeterli.", 23, WHITE);
        intro.setTypeface(null, Typeface.BOLD);
        text(root, "Sözcükler arasında durabilirsin. Komut için 30 saniyeye kadar bekler; konuştuğunda süre yenilenir. İstersen hiç konuşmadan düğmeleri kullan.", 16, MUTED);
        gap(root, 14);

        status = text(root, "Hazırlanıyor...", 17, ACCENT);
        voiceStatus = text(root, "Ses testi isteğe bağlı.", 15, MUTED);
        gap(root, 15);

        section(root, "MIA — Cihazda çalışan yapay zekâ");
        text(root, "1. GGUF modelini telefona indir. 2. Modeli seç. 3. Türkçe sorunu yaz. Soru ve yanıtlar sunucuya gönderilmez.", 15, MUTED);
        aiStatus = text(root, "Yapay zekâ modeli seçilmedi.", 15, ACCENT);
        button(root, "Yerel GGUF modelini seç", 0xff264a55, v -> chooseModel());
        button(root, "Önerilen ücretsiz modeli bul (1.5B)", 0xff243751, v ->
            open("https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/blob/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf"));
        aiInput = new EditText(this);
        aiInput.setSingleLine(false);
        aiInput.setMinLines(2);
        aiInput.setMaxLines(5);
        aiInput.setTextColor(WHITE);
        aiInput.setHintTextColor(0xff96a5bf);
        aiInput.setTextSize(18);
        aiInput.setHint("MIA'ya bir soru yaz...");
        aiInput.setPadding(dp(16), dp(12), dp(16), dp(12));
        aiInput.setBackground(background(0xff1b2b43, 16));
        root.addView(aiInput, new LinearLayout.LayoutParams(-1, -2));
        aiSend = button(root, "MIA'ya sor (yalnızca telefonda)", 0xff236d60, v -> askLocal());
        aiOutput = text(root, "Yanıt burada görünecek.", 17, WHITE);
        aiOutput.setTextIsSelectable(true);
        gap(root, 22);
        section(root, "Yazılı komut");
        typed = new EditText(this);
        typed.setSingleLine(true);
        typed.setTextColor(WHITE);
        typed.setHintTextColor(0xff96a5bf);
        typed.setTextSize(19);
        typed.setHint("Örn: Instagram aç");
        typed.setPadding(dp(18), dp(12), dp(18), dp(12));
        typed.setBackground(background(0xff1b2b43, 16));
        typed.setContentDescription("Mia için yazılı komut");
        root.addView(typed, new LinearLayout.LayoutParams(-1, dp(66)));
        button(root, "Komutu çalıştır", 0xff236d60, v -> runTyped());

        gap(root, 18);
        section(root, "Tek dokunuşla aç");
        button(root, "Instagram'ı aç", 0xff243751, v -> execute(CommandRouter.Kind.INSTAGRAM));
        button(root, "Play Console'u aç", 0xff243751, v -> execute(CommandRouter.Kind.PLAY_CONSOLE));
        button(root, "Web sitesini aç", 0xff243751, v -> execute(CommandRouter.Kind.WEBSITE));
        button(root, "Telefon aramasını aç", 0xff243751, v -> execute(CommandRouter.Kind.PHONE));

        gap(root, 18);
        section(root, "Sesli kullanım");
        button(root, "Hoparlörü test et", 0xff264a55, v -> {
            if (voice != null) {
                voiceStatus.setText("Test sesi başlatılıyor...");
                voice.say("Merhaba. Ben Mia. İstersen konuşabilir ya da yazabilirsin.",
                        () -> voiceStatus.setText("Ses testi tamamlandı. Duyulmadıysa medya sesini ve Bluetooth'u kontrol et."));
            }
        });
        downloadButton = button(root, "Türkçe modeli APK’dan kur", 0xff243751, v -> installModel());
        listenButton = button(root, "Mia'yı dinlemeye başlat", 0xff236d60, v -> requestStart());
        button(root, "Dinlemeyi durdur", 0xff533646, v -> stopListening());
        gap(root, 16);
        button(root, "Pil ayarlarını aç", 0xff243751,
                v -> startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        gap(root, 16);
        text(root, "Örnek: “Mia” ... [duraklama] ... “Instagram”", 16, WHITE);
        text(root, "Türkçe model artık uygulamaya dahildir; internet gerekmez. Ses anlaşılmasa bile yazılı komutlar ve büyük düğmeler çalışır. Hesap değişiklikleri henüz desteklenmiyor.", 14, MUTED);
        setContentView(scroll);
        voice = new VoiceSpeaker(this, message -> runOnUiThread(() -> voiceStatus.setText(message)));
        localAi = new LocalAiBridge(this, new LocalAiBridge.Listener() {
            @Override public void onStatus(String message) { aiStatus.setText(message); }
            @Override public void onToken(String token) {
                if (token.isEmpty()) generated.setLength(0);
                else generated.append(token);
                aiOutput.setText(generated.toString());
            }
            @Override public void onDone() { aiStatus.setText("Yanıt telefonda üretildi."); }
            @Override public void onError(String message) { aiStatus.setText(message); }
        });
        // Install the bundled model once on first launch, off the UI thread.
        if (!ModelInstaller.installed(this)) {
            installModel();
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 503);
        }
    }

    private void chooseModel() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            startActivityForResult(intent, REQ_GGUF);
        } catch (ActivityNotFoundException ex) {
            aiStatus.setText("Dosya seçici bulunamadı.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_GGUF || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri selected = data.getData();
        String name = "";
        try (Cursor c = getContentResolver().query(selected, null, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col >= 0) name = c.getString(col);
            }
        } catch (RuntimeException ignored) { }
        if (name == null || !name.toLowerCase(java.util.Locale.ROOT).endsWith(".gguf")) {
            aiStatus.setText("Lütfen .gguf uzantılı yerel yapay zekâ dosyası seç.");
            return;
        }
        aiStatus.setText("Model özel uygulama alanına kopyalanıyor...");
        workers.execute(() -> {
            try {
                File dir = new File(getFilesDir(), "local-ai");
                if (!dir.exists() && !dir.mkdirs()) throw new Exception("Model klasörü açılamadı");
                File tmp = new File(dir, "mia-model.part");
                File target = new File(dir, "mia-model.gguf");
                long copied = 0L;
                try (InputStream in = getContentResolver().openInputStream(selected);
                     FileOutputStream out = new FileOutputStream(tmp)) {
                    if (in == null) throw new Exception("Seçilen dosya okunamadı");
                    byte[] buf = new byte[1024 * 1024];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        if (n <= 0) continue;
                        copied += n;
                        if (copied > 5L * 1024L * 1024L * 1024L)
                            throw new Exception("Maksimum GGUF boyutu 5 GB");
                        out.write(buf, 0, n);
                        if (copied % (64L * 1024L * 1024L) < n) {
                            long mb = copied / (1024L * 1024L);
                            runOnUiThread(() -> aiStatus.setText("Model kopyalanıyor: " + mb + " MB"));
                        }
                    }
                } catch (Exception ex) {
                    tmp.delete();
                    throw ex;
                }
                if (copied < 16L * 1024L * 1024L) throw new Exception("GGUF dosyası eksik görünüyor");
                try (java.io.RandomAccessFile raf = new java.io.RandomAccessFile(tmp, "r")) {
                    byte[] magic = new byte[4];
                    raf.readFully(magic);
                    if (magic[0] != 'G' || magic[1] != 'G' || magic[2] != 'U' || magic[3] != 'F') {
                        throw new Exception("Seçilen dosya geçerli GGUF değil");
                    }
                }
                if (target.exists() && !target.delete()) throw new Exception("Eski model silinemedi");
                if (!tmp.renameTo(target)) throw new Exception("Model kaydedilemedi");
                runOnUiThread(() -> {
                    aiStatus.setText("GGUF doğrulandı. Yerel model yükleniyor...");
                    localAi.loadModel(target.getAbsolutePath());
                });
            } catch (Exception ex) {
                runOnUiThread(() -> aiStatus.setText("Model aktarımı başarısız: " + ex.getMessage()));
            }
        });
    }

    private void askLocal() {
        if (localAi == null) return;
        String question = aiInput.getText().toString().trim();
        if (question.isEmpty()) { aiStatus.setText("Önce soru yaz."); return; }
        aiStatus.setText("MIA çevrimdışı düşünüyor...");
        localAi.ask(question);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(aiInput.getWindowToken(), 0);
    }

    private void runTyped() {
        String input = typed.getText().toString();
        if (input.trim().isEmpty()) {
            status.setText("Bir komut yaz veya aşağıdaki düğmelerden birine dokun.");
            return;
        }
        CommandRouter.Kind kind = CommandRouter.classify(input);
        execute(kind);
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(typed.getWindowToken(), 0);
    }

    private void execute(CommandRouter.Kind kind) {
        try {
            switch (kind) {
                case INSTAGRAM:
                    open("https://www.instagram.com/");
                    status.setText("Instagram açıldı.");
                    break;
                case PLAY_CONSOLE:
                    open("https://play.google.com/console/");
                    status.setText("Play Console açıldı.");
                    break;
                case WEBSITE:
                    open("https://filtre.elxvro.com/");
                    status.setText("Web sitesi açıldı.");
                    break;
                case PHONE:
                    startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:")));
                    status.setText("Telefon numarası ekranı açıldı.");
                    break;
                case STOP:
                    stopListening();
                    break;
                case HELP:
                    status.setText("Kullanılabilir komutlar: Instagram, konsol, site, arama, dur.");
                    break;
                default:
                    status.setText("Komut anlaşılmadı. Tek kelime dene: Instagram, konsol, site veya arama.");
            }
        } catch (ActivityNotFoundException error) {
            status.setText("Bu işlemi açabilecek uygulama bulunamadı.");
        } catch (SecurityException error) {
            status.setText("Bu işlem için Android izni gerekiyor.");
        }
    }

    private void open(String url) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    @Override protected void onResume() {
        super.onResume();
        if (downloadButton != null) refreshModel();
    }

    private void refreshModel() {
        boolean installed = ModelInstaller.installed(this);
        downloadButton.setEnabled(!installed);
        listenButton.setEnabled(installed);
        if (installed) status.setText("Türkçe ses modeli hazır. Mia'yı başlatabilirsin.");
        else status.setText("Model kurulmadı. APK içindeki modeli yeniden kurabilirsin.");
    }

    private void installModel() {
        downloadButton.setEnabled(false);
        status.setText("Türkçe model APK içinden kuruluyor...");
        workers.execute(() -> {
            try {
                ModelInstaller.installBundled(this, message -> runOnUiThread(() -> status.setText(message)));
                runOnUiThread(this::refreshModel);
            } catch (Exception error) {
                runOnUiThread(() -> {
                    status.setText("Model kurulamadı: " + error.getMessage());
                    downloadButton.setEnabled(true);
                });
            }
        });
    }

    private void requestStart() {
        if (!ModelInstaller.installed(this)) { refreshModel(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
            return;
        }
        try {
            Intent intent = new Intent(this, JarvisService.class);
            intent.setAction(JarvisService.ACTION_START);
            startForegroundService(intent);
            status.setText("Mia dinliyor. Ekran kapalıyken Android izinleri geçerli.");
        } catch (RuntimeException error) {
            status.setText("Dinleme başlatılamadı: " + error.getMessage());
        }
    }

    private void stopListening() {
        stopService(new Intent(this, JarvisService.class));
        status.setText("Dinleme durduruldu.");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(requestCode, permissions, grants);
        if (requestCode == REQ_MIC) {
            if (grants.length > 0 && grants[0] == PackageManager.PERMISSION_GRANTED) requestStart();
            else status.setText("Mikrofon izni yok. Yazılı komutları kullanabilirsin.");
        }
    }

    private Button button(LinearLayout root, String caption, int color, View.OnClickListener action) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setText(caption);
        b.setTextSize(18);
        b.setTextColor(Color.WHITE);
        b.setContentDescription(caption);
        b.setBackground(background(color, 18));
        b.setOnClickListener(action);
        b.setMinHeight(dp(70));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(72));
        lp.topMargin = dp(9);
        root.addView(b, lp);
        return b;
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private void section(LinearLayout root, String name) {
        TextView t = text(root, name, 19, WHITE);
        t.setTypeface(null, Typeface.BOLD);
    }
    private void gap(LinearLayout root, int height) {
        root.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }
    private TextView text(LinearLayout root, String value, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setLineSpacing(dp(3), 1f);
        root.addView(t, new LinearLayout.LayoutParams(-1, -2));
        return t;
    }
    private int dp(int d) { return Math.round(getResources().getDisplayMetrics().density * d); }
    @Override protected void onDestroy() {
        workers.shutdown();
        if (voice != null) voice.shutdown();
        if (localAi != null) localAi.close();
        super.onDestroy();
    }
}
