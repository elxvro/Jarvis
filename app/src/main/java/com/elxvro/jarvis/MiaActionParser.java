package com.elxvro.jarvis;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A strictly allowlisted command layer. An LLM answer is NEVER parsed as an
 * executable instruction. Only a user's direct utterance is routed to tools,
 * and all state-changing or app-opening tools require a visible confirmation.
 */
public final class MiaActionParser {
    private MiaActionParser() {}

    public enum Type { NONE, INSTAGRAM, PLAY_CONSOLE, WEBSITE, YOUTUBE,
        DIAL, GOOGLE_SEARCH, SAVE_NOTE, LIST_NOTES, SETTINGS }

    public static final class Plan {
        public final Type type;
        public final String payload;
        public final String summary;

        Plan(Type type, String payload, String summary) {
            this.type = type;
            this.payload = payload;
            this.summary = summary;
        }
    }

    private static final Pattern NUMBER =
        Pattern.compile("^(?:telefon(?:dan)?\\s+|numara\\s+|ara\\s+|cevir\\s+|aramayi\\s+baslat\\s+)?(\\+?[0-9][0-9() +.\\-]{5,19})$");
    private static final Pattern WEB =
        Pattern.compile("^(?:google(?:'da|'de|da|de)?|internette|webde)\\s+(.+?)\\s+(?:ara|arat)$");

    public static Plan parse(String source) {
        if (source == null || source.trim().isEmpty()) return null;
        String raw = source.trim();
        String s = CommandRouter.stripWake(raw);
        String normalized = CommandRouter.normalize(s);
        if (normalized.isEmpty()) return null;

        if (normalized.equals("notlar") || normalized.equals("notlarim")
            || normalized.equals("notlarimi goster") || normalized.equals("notlarimi oku")) {
            return new Plan(Type.LIST_NOTES, "", "Kayıtlı notları göster");
        }

        String note = extractNote(raw);
        if (note != null && !note.trim().isEmpty()) {
            return new Plan(Type.SAVE_NOTE, note.trim(), "Telefona not kaydet: " + note.trim());
        }

        Matcher number = NUMBER.matcher(normalized);
        if (number.matches()) {
            String digits = number.group(1).replaceAll("[^+0-9]", "");
            if (digits.length() >= 7 && digits.length() <= 16
                    && digits.substring(1).indexOf('+') < 0) {
                return new Plan(Type.DIAL, digits, "Telefon arama ekranını aç: " + digits);
            }
        }

        if ((normalized.equals("telefon") || normalized.equals("arama")
                || normalized.equals("telefon ac") || normalized.equals("arama ac")
                || normalized.equals("arama yap") || normalized.equals("ara")
                || normalized.equals("telefonu ac") || normalized.equals("numara cevir"))) {
            return new Plan(Type.DIAL, "", "Telefon arama ekranını aç");
        }

        Matcher web = WEB.matcher(normalized);
        if (web.matches() && !web.group(1).trim().isEmpty()) {
            String query = web.group(1).trim();
            return new Plan(Type.GOOGLE_SEARCH, query, "Web'de ara: " + query);
        }

        if (normalized.startsWith("ara: ") && normalized.length() > 5) {
            return new Plan(Type.GOOGLE_SEARCH, normalized.substring(5), "Web'de ara: " + normalized.substring(5));
        }

        if (matchesLaunch(normalized, "instagram", "insta")) {
            return new Plan(Type.INSTAGRAM, "", "Instagram'ı aç");
        }
        if (matchesLaunch(normalized, "play console", "play konsol", "konsol")) {
            return new Plan(Type.PLAY_CONSOLE, "", "Google Play Console'u aç");
        }
        if (matchesLaunch(normalized, "site", "sitem", "sitemi", "web sitesi", "web site")) {
            return new Plan(Type.WEBSITE, "", "ELXVRO web sitesini aç");
        }
        if (matchesLaunch(normalized, "youtube", "yutub")) {
            return new Plan(Type.YOUTUBE, "", "YouTube'u aç");
        }
        if (matchesLaunch(normalized, "ayarlar", "telefon ayarlari")) {
            return new Plan(Type.SETTINGS, "", "Android ayarlarını aç");
        }
        return null;
    }

    private static boolean matchesLaunch(String input, String... targets) {
        for (String target : targets) {
            if (input.equals(target) || input.equals(target + "i")
                    || input.equals(target + "u")) return true;
            for (String suffix : new String[] {" ac", "i ac", "u ac", "'i ac", "'u ac",
                    "yi ac", "yu ac", "ye git", "a git", "'a git", "'e git",
                    " gir", "i goster", "u goster", " goster", " baslat"}) {
                if (input.equals(target + suffix)) return true;
            }
            if (input.equals("ac " + target) || input.equals("git " + target)) return true;
        }
        return false;
    }

    private static String extractNote(String raw) {
        String lower = raw.toLowerCase(new Locale("tr","TR")).trim();
        for (String prefix : new String[] {
                "not al:", "not al ", "not:", "not kaydet:", "not kaydet ",
                "mia not al:", "mia not al ", "miya not al:", "miya not al "}) {
            if (lower.startsWith(prefix)) {
                return raw.substring(prefix.length()).trim();
            }
        }
        return null;
    }
}
