package com.elxvro.jarvis;

/**
 * Holds a wake-word session during hesitant speech.
 * A pause of up to 30 seconds is permitted. Interim speech renews the window,
 * capped at 90 seconds total so Mia cannot remain unintentionally armed.
 * This class is pure Java and unit tested without Android.
 */
public final class WakeSession {
    public static final long PAUSE_MS = 30_000L;
    public static final long MAX_SESSION_MS = 90_000L;
    private boolean listeningForCommand;
    private long openedAt;
    private long lastSpeechAt;

    public boolean isArmed(long now) {
        if (!listeningForCommand) return false;
        if (now - lastSpeechAt >= PAUSE_MS || now - openedAt >= MAX_SESSION_MS) {
            cancel();
            return false;
        }
        return true;
    }

    public void wake(long now) {
        listeningForCommand = true;
        openedAt = now;
        lastSpeechAt = now;
    }

    public void heard(long now) {
        if (isArmed(now)) lastSpeechAt = now;
    }

    public long remainingMs(long now) {
        if (!isArmed(now)) return 0L;
        return Math.max(0L, Math.min(PAUSE_MS - (now - lastSpeechAt),
                MAX_SESSION_MS - (now - openedAt)));
    }

    public void cancel() {
        listeningForCommand = false;
        openedAt = 0L;
        lastSpeechAt = 0L;
    }
}
