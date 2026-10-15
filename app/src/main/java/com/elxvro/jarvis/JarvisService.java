package com.elxvro.jarvis;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

/**
 * User-started visible microphone foreground service. No covert or reboot
 * microphone restart. No unlock bypass, no unattended changes to accounts.
 */
public final class JarvisService extends Service implements RecognitionListener {
    public static final String ACTION_START = "com.elxvro.jarvis.action.START";
    public static final String ACTION_STOP = "com.elxvro.jarvis.action.STOP";
    private static final String CHANNEL = "jarvis_listening";
    private static final int NOTIFICATION_ID = 71;
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechService speech;
    private VoiceSpeaker voiceSpeaker;
    private boolean speaking;
    private Recognizer recognizer;
    private Model model;
    private PowerManager.WakeLock wakeLock;
    private boolean loading = false;
    private boolean awaitingCommand = false;
    private long suppressUntil = 0L;
    private Runnable commandTimeout;
    private final WakeSession wakeSession = new WakeSession();

    @Override public void onCreate() {
        super.onCreate();
        voiceSpeaker = new VoiceSpeaker(this, message -> {
            if (message.startsWith("Ses motoru bulunamadı")
                    || message.startsWith("Türkçe konuşma sesi eksik")
                    || message.startsWith("Konuşma motoru yanıt vermedi")) {
                update(message);
            }
        });
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel(CHANNEL, "JARVIS dinleme",
                NotificationManager.IMPORTANCE_LOW));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!ModelInstaller.installed(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, statusNotification("Ses modeli hazırlanıyor...", null),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(NOTIFICATION_ID, statusNotification("Ses modeli hazırlanıyor...", null));
        }
        if (!loading && speech == null) {
            loading = true;
            new Thread(this::initializeAudio, "jarvis-vosk-model").start();
        }
        return START_NOT_STICKY;
    }

    private void initializeAudio() {
        try {
            Model m = new Model(ModelInstaller.modelDir(this).getAbsolutePath());
            Recognizer r = new Recognizer(m, 16000f);
            main.post(() -> {
                if (!loading) { r.close(); m.close(); return; }
                model = m;
                recognizer = r;
                try {
                    speech = new SpeechService(recognizer, 16000f);
                    acquireWakeLock();
                    speech.startListening(this);
                    update("Dinliyor · Mia de");
                    speakWithPausedMic("Mia hazır. İstersen adımı söyle.", false);
                } catch (Exception error) {
                    update("Mikrofon açılamadı: " + error.getMessage());
                    stopSelf();
                }
            });
        } catch (Exception error) {
            main.post(() -> {
                update("Model açılamadı: " + error.getMessage());
                stopSelf();
            });
        }
    }

    private void acquireWakeLock() {
        PowerManager power = getSystemService(PowerManager.class);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "elxvro:jarvisListening");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire();
    }

    @Override public void onPartialResult(String hypothesis) { accept(hypothesis); }
    @Override public void onResult(String hypothesis) { accept(hypothesis); }
    @Override public void onFinalResult(String hypothesis) { accept(hypothesis); }
    @Override public void onError(Exception error) {
        update("Ses tanıma hatası: " + error.getMessage());
        stopSelf();
    }
    @Override public void onTimeout() { awaitingCommand = false; wakeSession.cancel(); update("Dinliyor · Mia de"); }

    private void accept(String rawJson) {
        if (speaking) return;
        try {
            JSONObject data = new JSONObject(rawJson);
            String recognized = data.optString("partial", data.optString("text", "")).trim();
            if (recognized.isEmpty()) return;
            long now = SystemClock.elapsedRealtime();
            if (awaitingCommand && !wakeSession.isArmed(now)) {
                awaitingCommand = false;
                update("Dinliyor · Mia de");
            }
            if (awaitingCommand) {
                String commandText = CommandRouter.stripWake(recognized);
                // Treat stuttered or incomplete words as extra thinking time.
                wakeSession.heard(now);
                if (commandText.isEmpty()) {
                    scheduleTimeout();
                    return;
                }
                CommandRouter.Kind kind = CommandRouter.classify(commandText);
                if (kind != CommandRouter.Kind.UNKNOWN) {
                    dispatch(kind);
                } else {
                    scheduleTimeout();
                }
                return;
            }
            if (now < suppressUntil) return;
            if (CommandRouter.containsWake(recognized)) {
                awaitingCommand = true;
                wakeSession.wake(now);
                update("Mia dinliyor. Acele etme, komutunu söyle.");
                String trailing = CommandRouter.stripWake(recognized);
                CommandRouter.Kind kind = CommandRouter.classify(trailing);
                if (kind != CommandRouter.Kind.UNKNOWN) {
                    dispatch(kind);
                } else {
                    speakWithPausedMic("Dinliyorum.", true);
                }
            }
        } catch (Exception error) {
            // Malformed partial speech is ignored. Never crash on incomplete input.
        }
    }

    private void scheduleTimeout() {
        if (commandTimeout != null) main.removeCallbacks(commandTimeout);
        if (!awaitingCommand) return;
        long left = wakeSession.remainingMs(SystemClock.elapsedRealtime());
        if (left <= 0L) {
            awaitingCommand = false;
            update("Dinliyor · Mia de");
            return;
        }
        commandTimeout = () -> {
            if (!awaitingCommand) return;
            if (!wakeSession.isArmed(SystemClock.elapsedRealtime())) {
                awaitingCommand = false;
                update("Komut süresi doldu. Mia diyebilirsin.");
                if (speech != null) speech.reset();
            } else {
                scheduleTimeout();
            }
        };
        main.postDelayed(commandTimeout, Math.max(250L, left));
    }

    private void dispatch(CommandRouter.Kind command) {
        awaitingCommand = false;
        wakeSession.cancel();
        suppressUntil = SystemClock.elapsedRealtime() + 4000L;
        if (commandTimeout != null) main.removeCallbacks(commandTimeout);
        if (speech != null) speech.reset();
        if (command == CommandRouter.Kind.STOP) {
            update("Mia durduruluyor...");
            speakWithPausedMic("Dinlemeyi kapatıyorum.", false, this::stopSelf);
            return;
        }
        String message;
        PendingIntent action = null;
        switch (command) {
            case HELP:
                message = "Komutlar: Instagram, Play Console, web sitesi, arama, dur.";
                break;
            case INSTAGRAM:
                message = "Instagram'ı açmak için bildirime dokun.";
                action = webAction("https://www.instagram.com/");
                break;
            case PLAY_CONSOLE:
                message = "Play Console'u açmak için bildirime dokun.";
                action = webAction("https://play.google.com/console/");
                break;
            case WEBSITE:
                message = "Web siteni açmak için bildirime dokun.";
                action = webAction("https://filtre.elxvro.com/");
                break;
            case PHONE:
                message = "Arama ekranı için bildirime dokun.";
                Intent dial = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
                dial.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                action = PendingIntent.getActivity(this, 25, dial,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                break;
            default:
                message = "Komut anlaşılamadı.";
        }
        update(message, action);
        String spokenReply;
        switch (command) {
            case HELP: spokenReply = "Instagram, Play Konsol, web sitesi, arama veya dur diyebilirsin."; break;
            case INSTAGRAM: spokenReply = "Instagram bağlantısını bildirimine ekledim."; break;
            case PLAY_CONSOLE: spokenReply = "Play Konsol bağlantısını bildirimine ekledim."; break;
            case WEBSITE: spokenReply = "Web sitesi bağlantısını bildirimine ekledim."; break;
            case PHONE: spokenReply = "Arama ekranını açman için bildirim hazırladım."; break;
            default: spokenReply = "Bu komutu henüz gerçekleştiremiyorum.";
        }
        speakWithPausedMic(spokenReply, false);
    }

    /**
     * Pause recognition while Android TTS speaks so Jarvis will not mistake its
     * own voice for a new command. Re-enable the microphone after completion.
     */
    private void speakWithPausedMic(String phrase, boolean expectNextCommand) {
        speakWithPausedMic(phrase, expectNextCommand, null);
    }

    private void speakWithPausedMic(String phrase, boolean expectNextCommand, Runnable completion) {
        speaking = true;
        if (speech != null) speech.setPause(true);
        if (voiceSpeaker == null) {
            finishSpeaking(expectNextCommand, completion);
        } else {
            voiceSpeaker.say(phrase, () -> finishSpeaking(expectNextCommand, completion));
        }
    }

    private void finishSpeaking(boolean expectNextCommand, Runnable completion) {
        speaking = false;
        if (speech != null) {
            speech.reset();
            speech.setPause(false);
        }
        if (expectNextCommand && awaitingCommand) scheduleTimeout();
        if (completion != null) completion.run();
    }

    private PendingIntent webAction(String destination) {
        Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse(destination));
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // Different data URIs keep these PendingIntents separate.
        return PendingIntent.getActivity(this, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void update(String status) { update(status, null); }
    private void update(String status, PendingIntent action) {
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, statusNotification(status, action));
    }

    private Notification statusNotification(String status, PendingIntent action) {
        Intent stop = new Intent(this, JarvisService.class);
        stop.setAction(ACTION_STOP);
        PendingIntent stopAction = PendingIntent.getService(this, 20, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("ELXVRO MIA")
                .setContentText(status)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .addAction(android.R.drawable.ic_media_pause, "Durdur", stopAction);
        if (action != null) {
            n.setContentIntent(action).setAutoCancel(false)
                    .addAction(android.R.drawable.ic_menu_view, "Aç", action);
        } else {
            Intent main = new Intent(this, MainActivity.class);
            n.setContentIntent(PendingIntent.getActivity(this, 21, main,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        return n.build();
    }

    @Override public void onDestroy() {
        loading = false;
        if (voiceSpeaker != null) {
            voiceSpeaker.shutdown();
            voiceSpeaker = null;
        }
        if (commandTimeout != null) main.removeCallbacks(commandTimeout);
        if (speech != null) {
            speech.shutdown();
            speech = null;
        }
        if (recognizer != null) { recognizer.close(); recognizer = null; }
        if (model != null) { model.close(); model = null; }
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
