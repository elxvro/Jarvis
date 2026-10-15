package com.elxvro.jarvis;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;

/**
 * Push-to-talk dictation entirely on the device. The user chooses when to
 * finish, so pauses/stuttering do not automatically send or execute commands.
 */
final class MiaDictation implements RecognitionListener {
    interface Listener {
        void status(String message);
        void text(String text);
        void stopped();
    }
    private final Context context;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Model model;
    private Recognizer recognizer;
    private SpeechService microphone;
    private boolean closed;
    private String committed = "";
    private String partial = "";

    MiaDictation(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    void start() {
        listener.status("Mikrofon hazırlanıyor...");
        new Thread(() -> {
            Model nextModel = null;
            Recognizer nextRecognizer = null;
            try {
                nextModel = new Model(ModelInstaller.modelDir(context).getAbsolutePath());
                nextRecognizer = new Recognizer(nextModel, 16000f);
                final Model fModel = nextModel;
                final Recognizer fRecognizer = nextRecognizer;
                main.post(() -> {
                    if (closed) {
                        fRecognizer.close();
                        fModel.close();
                        return;
                    }
                    model = fModel;
                    recognizer = fRecognizer;
                    try {
                        microphone = new SpeechService(recognizer, 16000f);
                        microphone.startListening(this);
                        listener.status("Mia dinliyor. Duraksayabilirsin; bitince 'Dinlemeyi bitir' de.");
                    } catch (Exception ex) {
                        listener.status("Mikrofon açılamadı: " + ex.getMessage());
                        close();
                    }
                });
            } catch (Exception error) {
                if (nextRecognizer != null) nextRecognizer.close();
                if (nextModel != null) nextModel.close();
                main.post(() -> {
                    if (!closed) {
                        listener.status("Yerel ses tanıma açılamadı: " + error.getMessage());
                        close();
                    }
                });
            }
        }, "mia-local-dictation").start();
    }

    private void updateText() {
        if (closed) return;
        String collected = (committed + " " + partial).trim().replaceAll("\\s+", " ");
        if (!collected.isEmpty()) listener.text(collected);
    }

    @Override public void onPartialResult(String json) {
        if (closed) return;
        try {
            partial = new JSONObject(json).optString("partial", "").trim();
            updateText();
        } catch (Exception ignored) { }
    }

    @Override public void onResult(String json) {
        if (closed) return;
        try {
            String finalText = new JSONObject(json).optString("text", "").trim();
            if (!finalText.isEmpty()) committed = (committed + " " + finalText).trim();
            partial = "";
            updateText();
        } catch (Exception ignored) { }
    }

    @Override public void onFinalResult(String json) { onResult(json); }
    @Override public void onError(Exception ex) {
        if (!closed) listener.status("Yerel ses tanıma hatası: " + ex.getMessage());
        close();
    }
    @Override public void onTimeout() {
        if (!closed) listener.status("Dinleme süresi doldu. Yeniden başlatabilirsin.");
        close();
    }

    void close() {
        if (closed) return;
        closed = true;
        main.post(() -> {
            if (microphone != null) {
                microphone.shutdown();
                microphone = null;
            }
            if (recognizer != null) {
                recognizer.close();
                recognizer = null;
            }
            if (model != null) {
                model.close();
                model = null;
            }
            listener.stopped();
        });
    }
}
