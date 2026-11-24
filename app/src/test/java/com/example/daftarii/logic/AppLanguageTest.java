package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AppLanguageTest {

    @Test
    public void fromTag_english() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"));
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"));
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("EN-gb"));
    }

    @Test
    public void fromTag_arabicAndOthers_defaultToArabic() {
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("ar"));
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("ar-PS"));
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag("fr-FR"));
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag(null));
        assertEquals(AppLanguage.ARABIC, AppLanguage.fromTag(""));
    }

    @Test
    public void other_toggles() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.ARABIC.other());
        assertEquals(AppLanguage.ARABIC, AppLanguage.ENGLISH.other());
    }

    @Test
    public void tags() {
        assertEquals("ar", AppLanguage.ARABIC.tag());
        assertEquals("en", AppLanguage.ENGLISH.tag());
    }
}
