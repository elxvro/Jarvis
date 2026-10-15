package com.elxvro.jarvis;

import org.junit.Test;
import static org.junit.Assert.*;

public class MiaActionParserTest {
    @Test public void launchesWithExactUserCommands() {
        assertEquals(MiaActionParser.Type.INSTAGRAM, MiaActionParser.parse("Mia Instagram aç").type);
        assertEquals(MiaActionParser.Type.PLAY_CONSOLE, MiaActionParser.parse("Play Console'u aç").type);
        assertEquals(MiaActionParser.Type.WEBSITE, MiaActionParser.parse("siteyi aç").type);
        assertEquals(MiaActionParser.Type.YOUTUBE, MiaActionParser.parse("YouTube aç").type);
        assertEquals(MiaActionParser.Type.SETTINGS, MiaActionParser.parse("ayarlar").type);
    }
    @Test public void neverTreatsQuestionsAsTools() {
        assertNull(MiaActionParser.parse("Instagram nasıl çalışıyor?"));
        assertNull(MiaActionParser.parse("Play Console için uygulama fikri ver"));
        assertNull(MiaActionParser.parse("Bana Python kodu yaz"));
        assertNull(MiaActionParser.parse("Hesabımdan para gönder"));
        assertNull(MiaActionParser.parse("Mia nasılsın?"));
    }
    @Test public void canTakeNotesLocally() {
        MiaActionParser.Plan plan = MiaActionParser.parse("Not al: yarın elxvro sitesini güncelle");
        assertEquals(MiaActionParser.Type.SAVE_NOTE, plan.type);
        assertEquals("yarın elxvro sitesini güncelle", plan.payload);
        assertEquals(MiaActionParser.Type.LIST_NOTES, MiaActionParser.parse("Notlarımı göster").type);
    }
    @Test public void opensDialerButDoesNotCall() {
        MiaActionParser.Plan plan = MiaActionParser.parse("0532 123 45 67");
        assertEquals(MiaActionParser.Type.DIAL, plan.type);
        assertEquals("05321234567", plan.payload);
    }
    @Test public void webSearchIsExplicit() {
        MiaActionParser.Plan plan = MiaActionParser.parse("google'da Android haberleri ara");
        assertEquals(MiaActionParser.Type.GOOGLE_SEARCH, plan.type);
        assertEquals("android haberleri", plan.payload);
    }
}
