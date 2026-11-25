package com.example.daftarii.auth;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * خروج الزائر (من حسابي). الزائر ما إله PIN، بس بنمسح ملف الـ PIN احتياطاً.
 *
 * ملاحظة: كان هون "الزائر مرة وحدة بس على الجهاز". انشال بقرار الـ PO:
 * الزائر إله 7 أيام تجربة (GuestTrial)، وبعدها لازم يعمل حساب، وبياناته بتضل.
 */
public class GuestSessionHelper {

    public GuestSessionHelper(@NonNull Context context) {
        // ما في حالة محفوظة. الـ constructor ضل عشان حسابي بيستخدمه.
    }

    /** إنهاء جلسة الزائر الحالية ومسح بيانات القفل المحلية. */
    public void endGuestSessionAndClearPin(@NonNull Context context) {
        new PinStore(context).clear();
        new AuthRepository().signOut();
    }
}
