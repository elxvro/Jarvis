package com.elxvro.jarvis;

import static org.junit.Assert.*;
import org.junit.Test;

public final class CommandRouterTest {
    @Test public void recognizesWake() {
        assertTrue(CommandRouter.containsWake("hey jarvis"));
        assertTrue(CommandRouter.containsWake("Hey Jarvis instagram"));
        assertFalse(CommandRouter.containsWake("instagram"));
    }
    @Test public void routesTurkishCommands() {
        assertEquals(CommandRouter.Kind.INSTAGRAM, CommandRouter.classify("instagram aç"));
        assertEquals(CommandRouter.Kind.PLAY_CONSOLE, CommandRouter.classify("play konsol"));
        assertEquals(CommandRouter.Kind.WEBSITE, CommandRouter.classify("web sitesi"));
        assertEquals(CommandRouter.Kind.PHONE, CommandRouter.classify("arama yap"));
        assertEquals(CommandRouter.Kind.STOP, CommandRouter.classify("dinlemeyi durdur"));
        assertEquals(CommandRouter.Kind.UNKNOWN, CommandRouter.classify("para gönder"));
    }
}
