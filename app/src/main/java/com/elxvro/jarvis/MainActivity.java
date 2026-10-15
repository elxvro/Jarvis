package com.elxvro.jarvis;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int REQUEST_MIC = 502;
    private TextView status;
    private TextView voiceStatus;
    private VoiceSpeaker voiceSpeaker;
    private Button startButton;
    private Button installButton;
    private final ExecutorService workers = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(12, 18, 31));
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(22), dp(34), dp(22), dp(22));
        scroll.addView(column);

        title(column, "ELXVRO", 28, 0xff53dec1);
        title(column, "JARVIS", 42, Color.WHITE);
        text(column, "v0.1 • Kişisel, çevrimdışı sesli asistan", 14, 0xffa2aec4);
        space(column, 24);
        title(column, "Hey Jarvis", 25, Color.WHITE);
        text(column, "Dinlemeyi uygulama açıkken başlat. Ekranı kapatsan da Android izin verdiği sürece komutları algılar.", 15, 0xffd2dbeb);
        space(column, 16);
        status = text(column, "", 15, 0xff53dec1);
        voiceStatus = text(column, "Türkçe ses hazırlanıyor...", 14, 0xffa9c5dc);
        space(column, 16);

        button(column, "🔊  Sesi test et", v -> {
            if (voiceSpeaker != null) {
                voiceSpeaker.say("Merhaba. Ben ELXVRO Jarvis. Seni duyuyorum ve sana Türkçe cevap verebilirim.",
                        () -> voiceStatus.setText("Ses testi tamamlandı."));
            }
        });
        button(column, "Türkçe ses paketini yükle / ayarla", v -> {
            try {
                startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
            } catch (RuntimeException ex) {
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        installButton = button(column, "1  ·  Türkçe tanıma modelini indir", v -> installModel());
        startButton = button(column, "2  ·  Dinlemeyi başlat", v -> requestStart());
        button(column, "Dinlemeyi durdur", v -> stopListening());
        button(column, "Pil ayarlarını aç", v -> {
            startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
        });
        space(column, 20);
        title(column, "Örnek komutlar", 19, Color.WHITE);
        text(column, "“Hey Jarvis” → “Instagram”\n“Hey Jarvis” → “Play Console”\n“Hey Jarvis” → “Web sitesi”\n“Hey Jarvis” → “Arama”\n“Hey Jarvis” → “Dur”", 16, 0xffc5cedc);
        space(column, 18);
        text(column, "Güvenlik: Ses kaydı tutulmaz. Hesap şifresi istenmez. Bu sürüm bağlantıları bildirimle açar; hesaplarda değişiklik yapmaz.", 13, 0xff95a4bb);
        setContentView(scroll);
        voiceSpeaker = new VoiceSpeaker(this, message -> voiceStatus.setText(message));
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 503);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void refreshStatus() {
        boolean installed = ModelInstaller.installed(this);
        status.setText(installed ? "Ses modeli hazır." : "İlk kullanım için Türkçe ses modelini indir.");
        installButton.setEnabled(!installed);
        startButton.setEnabled(installed);
    }

    private void installModel() {
        installButton.setEnabled(false);
        startButton.setEnabled(false);
        workers.execute(() -> {
            try {
                ModelInstaller.downloadAndInstall(this, message -> runOnUiThread(() -> status.setText(message)));
                runOnUiThread(this::refreshStatus);
            } catch (Exception error) {
                runOnUiThread(() -> {
                    status.setText("Model hatası: " + error.getMessage());
                    installButton.setEnabled(true);
                });
            }
        });
    }

    private void requestStart() {
        if (!ModelInstaller.installed(this)) { refreshStatus(); return; }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            return;
        }
        Intent start = new Intent(this, JarvisService.class);
        start.setAction(JarvisService.ACTION_START);
        // Must be called while this Activity is visible (microphone WIU restriction).
        startForegroundService(start);
        status.setText("Dinleme başlatıldı. Kalıcı bildirimi kontrol et.");
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) requestStart();
            else status.setText("Dinleme için mikrofon izni gerekir.");
        }
    }

    private void stopListening() {
        Intent stop = new Intent(this, JarvisService.class);
        stop.setAction(JarvisService.ACTION_STOP);
        startService(stop);
        status.setText("Dinleme durduruluyor.");
    }

    @Override protected void onDestroy() {
        if (voiceSpeaker != null) voiceSpeaker.shutdown();
        workers.shutdown();
        super.onDestroy();
    }

    private int dp(int value) { return Math.round(getResources().getDisplayMetrics().density * value); }

    private void space(LinearLayout box, int px) {
        View blank = new View(this);
        box.addView(blank, new LinearLayout.LayoutParams(1, dp(px)));
    }

    private TextView title(LinearLayout root, String message, int sp, int color) {
        TextView tv = text(root, message, sp, color);
        tv.setTypeface(null, 1);
        return tv;
    }

    private TextView text(LinearLayout root, String message, int sp, int color) {
        TextView view = new TextView(this);
        view.setText(message);
        view.setTextColor(color);
        view.setTextSize(sp);
        view.setLineSpacing(dp(3), 1f);
        root.addView(view, new LinearLayout.LayoutParams(-1, -2));
        return view;
    }

    private Button button(LinearLayout root, String label, View.OnClickListener action) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setOnClickListener(action);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(58));
        params.topMargin = dp(8);
        root.addView(button, params);
        return button;
    }
}
