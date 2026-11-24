package com.example.daftarii.util;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.example.daftarii.logic.AppLanguage;

import java.util.Locale;

/**
 * تغيير لغة التطبيق من جوا التطبيق، بدون ما تتغيّر لغة الجوال.
 * أندرويد بيحفظ الاختيار لحاله (autoStoreLocales بالـ Manifest)، وبيعيد فتح الشاشات باللغة الجديدة.
 */
public final class LanguageManager {

    private LanguageManager() {}

    /** اللغة الحالية: اللي اختارها المستخدم، وإذا ما اختار، لغة الجوال. */
    public static AppLanguage current() {
        LocaleListCompat appLocales = AppCompatDelegate.getApplicationLocales();
        if (!appLocales.isEmpty() && appLocales.get(0) != null) {
            return AppLanguage.fromTag(appLocales.get(0).toLanguageTag());
        }
        return AppLanguage.fromTag(Locale.getDefault().toLanguageTag());
    }

    public static void set(AppLanguage language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag()));
    }

    /** زر تغيير اللغة: من العربي للإنجليزي وبالعكس. */
    public static void toggle() {
        set(current().other());
    }
}
