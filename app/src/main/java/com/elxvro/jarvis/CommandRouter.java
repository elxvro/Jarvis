package com.elxvro.jarvis;

import java.util.Locale;

/** Whitelisted, local-only commands. No remote account actions in v0.1. */
public final class CommandRouter {
    private CommandRouter() {}

    public enum Kind { STOP, HELP, INSTAGRAM, PLAY_CONSOLE, WEBSITE, PHONE, UNKNOWN }

    public static Kind classify(String command) {
        if (command == null) return Kind.UNKNOWN;
        String s = command.toLowerCase(new Locale("tr", "TR"))
                .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
                .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
                .trim();
        if (s.equals("dur") || s.contains("dinlemeyi durdur") || s.contains("jarvis kapat")) return Kind.STOP;
        if (s.contains("yardim") || s.contains("ne yapabili")) return Kind.HELP;
        if (s.contains("instagram")) return Kind.INSTAGRAM;
        if (s.contains("play console") || s.contains("play konsol") || s.contains("uygulama magazasi")) return Kind.PLAY_CONSOLE;
        if (s.contains("web sitesi") || s.contains("web site") || s.contains("siteyi") || s.equals("site")) return Kind.WEBSITE;
        if (s.contains("arama") || s.endsWith(" ara") || s.equals("ara")) return Kind.PHONE;
        return Kind.UNKNOWN;
    }

    public static boolean containsWake(String text) {
        if (text == null) return false;
        String s = text.toLowerCase(Locale.ROOT);
        return s.contains("hey jarvis") || s.contains("hey carvis") || s.contains("hey carvıs");
    }

    public static String stripWake(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replace("hey jarvis", "")
                .replace("hey carvis", "")
                .replace("hey carvıs", "")
                .trim();
    }
}
