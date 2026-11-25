package com.example.daftarii.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class GuestSessionHelper {

    private static final String PREFS_NAME = "guest_session_prefs";
    private static final String KEY_GUEST_USED_ONCE = "guest_used_once";
    private static final String KEY_GUEST_UID = "guest_uid";

    private final SharedPreferences prefs;

    public GuestSessionHelper(@NonNull Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /** هل سبق واستُخدم وضع الزائر مرة واحدة على هذا الجهاز؟ */
    public boolean hasUsedGuestOnce() {
        return prefs.getBoolean(KEY_GUEST_USED_ONCE, false);
    }

    /** تسجيل أن وضع الزائر تم استخدامه مرة واحدة محلياً وفي Firestore. */
    public void markGuestUsedOnce(@NonNull String uid) {
        prefs.edit()
                .putBoolean(KEY_GUEST_USED_ONCE, true)
                .putString(KEY_GUEST_UID, uid)
                .apply();

        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("isAnonymous", true);
        map.put("guestUsedOnce", true);
        map.put("guestSessionStartedAt", System.currentTimeMillis());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .set(map, SetOptions.merge());
    }

    /** إنهاء جلسة الزائر الحالية ومسح بيانات القفل المحلية. */
    public void endGuestSessionAndClearPin(@NonNull Context context) {
        new PinStore(context).clear();
        FirebaseAuth.getInstance().signOut();
    }
}