package com.elxvro.jarvis;

import android.content.Context;
import android.media.AudioAttributes;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import java.util.Locale;
import java.util.Set;

/**
 * Uses the device's installed speech engine. Does not upload transcripts or
 * synthesize speech on a server owned by ELXVRO. Prefers Turkish offline voices.
 */
final class VoiceSpeaker {
    interface StatusListener { void onStatus(String message); }

    private final Handler main = new Handler(Looper.getMainLooper());
    private final StatusListener listener;
    private final BundledVoicePlayer bundled;
    private TextToSpeech tts;
    private boolean initialized;
    private boolean ready;
    private boolean released;
    private String waitingText;
    private Runnable waitingCompletion;
    private String activeId;
    private Runnable activeCompletion;
    private long sequence;

    VoiceSpeaker(Context context, StatusListener listener) {
        this.listener = listener;
        this.bundled = new BundledVoicePlayer(context, listener::onStatus);
        listener.onStatus("Ses motoru hazırlanıyor...");
        tts = new TextToSpeech(context.getApplicationContext(),
                result -> main.post(() -> onInitialized(result)));
    }

    private void onInitialized(int result) {
        if (released) return;
        initialized = true;
        if (result == TextToSpeech.SUCCESS && tts != null) {
            int language = tts.setLanguage(new Locale("tr", "TR"));
            if (language >= TextToSpeech.LANG_AVAILABLE) {
                ready = true;
                tts.setSpeechRate(1.0f);
                tts.setPitch(1.0f);
                tts.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
                // Prefer an installed on-device Turkish voice where available.
                try {
                    Set<Voice> voices = tts.getVoices();
                    if (voices != null) {
                        for (Voice voice : voices) {
                            if ("tr".equals(voice.getLocale().getLanguage())
                                    && !voice.isNetworkConnectionRequired()) {
                                tts.setVoice(voice);
                                break;
                            }
                        }
                    }
                } catch (RuntimeException ignored) {
                    // Some OEM engines do not expose a voice enumeration.
                }
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) { }
                    @Override public void onDone(String id) {
                        main.post(() -> finish(id));
                    }
                    @Override public void onError(String id) {
                        main.post(() -> {
                            if (!released) listener.onStatus("Ses oynatılamadı; medya sesini ve TTS motorunu kontrol et.");
                            finish(id);
                        });
                    }
                });
                listener.onStatus("Türkçe ses hazır. 'Sesi test et' düğmesine dokun.");
            } else {
                listener.onStatus("Yerleşik Türkçe ses çalışabilir; dinamik konuşma için Türkçe TTS paketi eksik.");
            }
        } else {
            listener.onStatus("Yerleşik Türkçe ses çalışabilir; sistem konuşma motoru eksik.");
        }
        if (waitingText != null) {
            String text = waitingText;
            Runnable callback = waitingCompletion;
            waitingText = null;
            waitingCompletion = null;
            say(text, callback);
        }
    }

    void say(String phrase, Runnable afterSpeaking) {
        if (released) {
            if (afterSpeaking != null) afterSpeaking.run();
            return;
        }
        if (bundled.has(phrase)) {
            bundled.play(phrase, afterSpeaking);
            return;
        }
        if (!initialized) {
            // Only the most recent request is relevant before engine init.
            waitingText = phrase;
            waitingCompletion = afterSpeaking;
            main.postDelayed(() -> {
                if (waitingText != null && !initialized) {
                    Runnable callback = waitingCompletion;
                    waitingText = null;
                    waitingCompletion = null;
                    listener.onStatus("Konuşma motoru yanıt vermedi. TTS ayarlarını kontrol et.");
                    if (callback != null) callback.run();
                }
            }, 5500L);
            return;
        }
        if (!ready || tts == null) {
            if (afterSpeaking != null) afterSpeaking.run();
            return;
        }
        if (activeCompletion != null) {
            // QUEUE_FLUSH interrupts the older phrase.
            Runnable old = activeCompletion;
            activeCompletion = null;
            activeId = null;
            old.run();
        }
        String id = "jarvis-" + (++sequence);
        activeId = id;
        activeCompletion = afterSpeaking == null ? () -> { } : afterSpeaking;
        int result = tts.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, id);
        if (result == TextToSpeech.ERROR) {
            listener.onStatus("Ses motoru oynatmayı başlatamadı.");
            finish(id);
        } else {
            main.postDelayed(() -> finish(id), 12000L);
        }
    }

    private void finish(String id) {
        if (released || id == null || !id.equals(activeId)) return;
        activeId = null;
        Runnable callback = activeCompletion;
        activeCompletion = null;
        if (callback != null) callback.run();
    }

    void shutdown() {
        released = true;
        bundled.close();
        waitingText = null;
        waitingCompletion = null;
        activeCompletion = null;
        activeId = null;
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }
}
