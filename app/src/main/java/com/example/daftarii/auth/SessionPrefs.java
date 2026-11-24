package com.example.daftarii.auth;

import android.content.Context;
import android.content.SharedPreferences;

/** إعدادات بسيطة للجلسة محفوظة على الجهاز: "تذكرني" و"شاف الترحيب". */
public class SessionPrefs {

    private static final String FILE = "daftari_session";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";

    private final SharedPreferences prefs;

    public SessionPrefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean isRememberMe() {
        return prefs.getBoolean(KEY_REMEMBER_ME, false);
    }

    public void setRememberMe(boolean value) {
        prefs.edit().putBoolean(KEY_REMEMBER_ME, value).apply();
    }

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean value) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply();
    }
}
