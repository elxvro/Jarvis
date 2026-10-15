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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Accessible, large-touch-target UI; fully usable with no voice input. */
public final class MainActivity extends Activity {
    private static final int REQ_MIC = 502;
    private static final int WHITE = 0xffedf2fa;
    private static final int MUTED = 0xffa6b7d0;
    private static final int ACCENT = 0xff5ce3c0;
    private final ExecutorService workers = Executors.newSingleThreadExecutor();
    private TextView status, voiceStatus;
    private EditText typed;
    private VoiceSpeaker voice;
    private Button downloadButton, listenButton;

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
        text(root, "v0.2.1 · Hazır Türkçe model · Büyük düğmeler", 14, MUTED);
        gap(root, 18);
        TextView intro = text(root, "Sadece “Mia” demen yeterli.", 23, WHITE);
        intro.setTypeface(null, Typeface.BOLD);
        text(root, "Sözcükler arasında durabilirsin. Komut için 30 saniyeye kadar bekler; konuştuğunda süre yenilenir. İstersen hiç konuşmadan düğmeleri kullan.", 16, MUTED);
        gap(root, 14);

        status = text(root, "Hazırlanıyor...", 17, ACCENT);
        voiceStatus = text(root, "Ses testi isteğe bağlı.", 15, MUTED);
        gap(root, 15);

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
        // Install the bundled model once on first launch, off the UI thread.
        if (!ModelInstaller.installed(this)) {
            installModel();
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 503);
        }
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
        super.onDestroy();
    }
}
