package com.example.daftarii.auth;

import android.content.Context;
import android.content.SharedPreferences;

/** إعدادات بسيطة للجلسة محفوظة على الجهاز: "تذكرني" و"شاف الترحيب" و"زائر حوّل لحساب ولسا ما فعّل". */
public class SessionPrefs {

    private static final String FILE = "daftari_session";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_UPGRADED_GUEST = "upgraded_guest_pending";

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

    /**
     * زائر حوّل لحساب ولسا ما فعّل الإيميل. شاشة التفعيل بتستخدمه عشان "تغيير الإيميل"
     * يرجّعه زائر بدل ما يحذف الحساب (وتضيع بياناته).
     */
    public boolean isUpgradedGuestPending() {
        return prefs.getBoolean(KEY_UPGRADED_GUEST, false);
    }

    public void setUpgradedGuestPending(boolean value) {
        prefs.edit().putBoolean(KEY_UPGRADED_GUEST, value).apply();
    }
}
