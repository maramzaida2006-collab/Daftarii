package com.example.daftarii;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;

/**
 * أول إشي بيشتغل لما التطبيق يفتح: بيطبّق الوضع الليلي المحفوظ من حسابي.
 * (قبل كان بيرجع فاتح كل ما التطبيق يتسكّر.) اللغة أندرويد بيحفظها لحاله (LanguageManager).
 */
public class DaftariApp extends Application {

    /** نفس الملف والمفتاح اللي بتستخدمهم صفحة حسابي. */
    public static final String PREFS = "daftarii_prefs";
    public static final String KEY_DARK_MODE = "dark_mode";

    @Override
    public void onCreate() {
        super.onCreate();
        boolean dark = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_DARK_MODE, false);
        AppCompatDelegate.setDefaultNightMode(dark
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }
}
