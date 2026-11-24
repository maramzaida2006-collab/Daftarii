package com.example.daftarii.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.example.daftarii.logic.PinAttempts;
import com.example.daftarii.logic.PinHasher;

/**
 * مكان حفظ الـ PIN على الجهاز، مربوط بالـ uid تبع الحساب.
 * ما بنحفظ الـ PIN نفسه: بنحفظ بصمته (hash) مع ملح عشوائي (PinHasher).
 * الملف خاص بالتطبيق (MODE_PRIVATE)، وما بيدخل بالنسخ الاحتياطي (backup_rules).
 * تسجيل الخروج بيمسح كل إشي هون (clear).
 */
public class PinStore {

    private static final String FILE = "daftari_pin";
    private static final String KEY_HASH = "pin_hash_";
    private static final String KEY_SALT = "pin_salt_";
    private static final String KEY_BIOMETRIC = "biometric_";
    private static final String KEY_FAILURES = "failures_";
    private static final String KEY_LOCKED_UNTIL = "locked_until_";

    private final SharedPreferences prefs;

    public PinStore(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public boolean hasPin(@NonNull String uid) {
        return prefs.contains(KEY_HASH + uid) && prefs.contains(KEY_SALT + uid);
    }

    /** PIN جديد: ملح جديد وبصمة جديدة، والمحاولات بتتصفّر. */
    public void savePin(@NonNull String uid, @NonNull String pin) {
        String salt = PinHasher.newSalt();
        prefs.edit()
                .putString(KEY_SALT + uid, salt)
                .putString(KEY_HASH + uid, PinHasher.hash(pin, salt))
                .remove(KEY_FAILURES + uid)
                .remove(KEY_LOCKED_UNTIL + uid)
                .apply();
    }

    public boolean checkPin(@NonNull String uid, @NonNull String pin) {
        return PinHasher.matches(pin,
                prefs.getString(KEY_SALT + uid, null),
                prefs.getString(KEY_HASH + uid, null));
    }

    public boolean isBiometricEnabled(@NonNull String uid) {
        return prefs.getBoolean(KEY_BIOMETRIC + uid, false);
    }

    public void setBiometricEnabled(@NonNull String uid, boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC + uid, enabled).apply();
    }

    @NonNull
    public PinAttempts.State attempts(@NonNull String uid) {
        return new PinAttempts.State(
                prefs.getInt(KEY_FAILURES + uid, 0),
                prefs.getLong(KEY_LOCKED_UNTIL + uid, 0));
    }

    public void saveAttempts(@NonNull String uid, @NonNull PinAttempts.State state) {
        prefs.edit()
                .putInt(KEY_FAILURES + uid, state.failures)
                .putLong(KEY_LOCKED_UNTIL + uid, state.lockedUntil)
                .apply();
    }

    /** مع تسجيل الخروج: بيمسح الـ PIN والبصمة لكل الحسابات على الجهاز. */
    public void clear() {
        prefs.edit().clear().apply();
    }
}
