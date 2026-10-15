package com.elxvro.jarvis;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

/** Bundled Turkish voice snippets; requires no system TextToSpeech installation. */
final class BundledVoicePlayer {
    interface Status { void set(String status); }
    private final Context context;
    private final Status status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String,String> phrases = new HashMap<>();
    private MediaPlayer current;
    private Runnable callback;
    private int sequence;
    private boolean closed;

    BundledVoicePlayer(Context context, Status status) {
        this.context = context.getApplicationContext();
        this.status = status;
        phrases.put("Merhaba. Ben ELXVRO Jarvis. Seni duyuyorum ve sana Türkçe cevap verebilirim.", "test.wav");
        phrases.put("Jarvis hazır. Hey Jarvis diyebilirsin.", "ready.wav");
        phrases.put("Dinliyorum.", "listening.wav");
        phrases.put("Dinlemeyi kapatıyorum.", "stopping.wav");
        phrases.put("Instagram, Play Konsol, web sitesi, arama veya dur diyebilirsin.", "help.wav");
        phrases.put("Instagram bağlantısını bildirimine ekledim.", "instagram.wav");
        phrases.put("Play Konsol bağlantısını bildirimine ekledim.", "play.wav");
        phrases.put("Web sitesi bağlantısını bildirimine ekledim.", "website.wav");
        phrases.put("Arama ekranını açman için bildirim hazırladım.", "phone.wav");
        phrases.put("Bu komutu henüz gerçekleştiremiyorum.", "unknown.wav");
    }
    boolean has(String sentence) { return phrases.containsKey(sentence); }

    void play(String sentence, Runnable onDone) {
        handler.post(() -> {
            if (closed) { if (onDone != null) onDone.run(); return; }
            finish(null);
            int id = ++sequence;
            callback = onDone;
            try {
                MediaPlayer player = new MediaPlayer();
                current = player;
                player.setAudioAttributes(new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build());
                try (AssetFileDescriptor file = context.getAssets().openFd("voice/" + phrases.get(sentence))) {
                    player.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                }
                player.setOnCompletionListener(mp -> handler.post(() -> {
                    if (id == sequence) finish(null);
                }));
                player.setOnErrorListener((mp, what, extra) -> {
                    handler.post(() -> { if (id == sequence) finish("Ses çalma hatası: " + what); });
                    return true;
                });
                player.prepare();
                player.setVolume(1f, 1f);
                player.start();
                status.set("Yerleşik Türkçe ses oynatılıyor (medya sesi).");
            } catch (Exception ex) {
                finish("Ses dosyası açılamadı: " + ex.getMessage());
            }
        });
    }

    private void finish(String error) {
        ++sequence;
        MediaPlayer previous = current;
        current = null;
        Runnable completed = callback;
        callback = null;
        if (previous != null) {
            try { previous.reset(); } catch (RuntimeException ignored) { }
            previous.release();
        }
        if (error != null) status.set(error);
        if (completed != null) completed.run();
    }

    void close() {
        handler.post(() -> { closed = true; finish(null); });
    }
}
