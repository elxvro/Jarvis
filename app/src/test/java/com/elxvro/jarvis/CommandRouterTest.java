package com.elxvro.jarvis;

import static org.junit.Assert.*;
import org.junit.Test;

public final class CommandRouterTest {
    @Test public void oneWordWake() {
        assertTrue(CommandRouter.containsWake("Mia"));
        assertTrue(CommandRouter.containsWake("miya"));
        assertTrue(CommandRouter.containsWake("Mia instagram"));
        assertFalse(CommandRouter.containsWake("kimyasal"));
        assertFalse(CommandRouter.containsWake("instagram"));
        assertFalse(CommandRouter.containsWake("hey jarvis"));
    }

    @Test public void commandsWorkWithoutSpeaking() {
        assertEquals(CommandRouter.Kind.INSTAGRAM, CommandRouter.classify("Instagram aç"));
        assertEquals(CommandRouter.Kind.INSTAGRAM, CommandRouter.classify("Mia... instagram"));
        assertEquals(CommandRouter.Kind.PLAY_CONSOLE, CommandRouter.classify("konsol"));
        assertEquals(CommandRouter.Kind.WEBSITE, CommandRouter.classify("site"));
        assertEquals(CommandRouter.Kind.PHONE, CommandRouter.classify("arama yap"));
        assertEquals(CommandRouter.Kind.STOP, CommandRouter.classify("dur"));
        assertEquals(CommandRouter.Kind.UNKNOWN, CommandRouter.classify("para gönder"));
    }

    @Test public void wakeSessionAllowsLongPauses() {
        WakeSession w = new WakeSession();
        w.wake(10000L);
        assertTrue(w.isArmed(39000L));
        w.heard(38000L);
        assertTrue(w.isArmed(65000L));
        assertFalse(w.isArmed(70000L));
    }

    @Test public void wakeSessionHasMaximumDuration() {
        WakeSession w = new WakeSession();
        w.wake(1000L);
        w.heard(20000L);
        w.heard(40000L);
        w.heard(60000L);
        w.heard(80000L);
        assertFalse(w.isArmed(91000L));
    }
}
