package com.elxvro.jarvis;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Small and predictable offline command router. All commands can also be typed
 * or selected using a button, without requiring speech recognition.
 */
public final class CommandRouter {
    private CommandRouter() {}
    public enum Kind { STOP, HELP, INSTAGRAM, PLAY_CONSOLE, WEBSITE, PHONE, UNKNOWN }

    private static final Pattern MIA = Pattern.compile(
            "(?iu)(?<![a-z])(?:mia|miya|miyya|miiya)(?![a-z])");

    public static String normalize(String text) {
        if (text == null) return "";
        return text.toLowerCase(new Locale("tr", "TR"))
                .replace('ı', 'i').replace('ş', 's').replace('ğ', 'g')
                .replace('ü', 'u').replace('ö', 'o').replace('ç', 'c')
                .trim().replaceAll("\\s+", " ");
    }

    public static boolean containsWake(String text) {
        return MIA.matcher(normalize(text)).find();
    }

    public static String stripWake(String text) {
        return MIA.matcher(normalize(text)).replaceAll(" ").trim().replaceAll("\\s+", " ");
    }

    public static Kind classify(String command) {
        String s = stripWake(command);
        if (s.isEmpty()) return Kind.UNKNOWN;
        if (s.equals("dur") || s.equals("kapat") || s.contains("dinlemeyi durdur")
                || s.contains("mia kapat") || s.contains("dinleme kapat")) return Kind.STOP;
        if (s.equals("yardim") || s.contains("ne yapabili") || s.contains("komutlar")) return Kind.HELP;
        if (s.contains("instagram") || s.equals("insta")) return Kind.INSTAGRAM;
        if (s.contains("play console") || s.contains("play konsol")
                || s.contains("uygulama magazasi") || s.equals("konsol")) return Kind.PLAY_CONSOLE;
        if (s.contains("web sitesi") || s.contains("web site") || s.contains("siteyi")
                || s.equals("site") || s.equals("web")) return Kind.WEBSITE;
        if (s.contains("arama") || s.equals("telefon") || s.endsWith(" ara")
                || s.equals("ara")) return Kind.PHONE;
        return Kind.UNKNOWN;
    }
}
