package com.example.daftarii.auth;

import android.content.Context;

import androidx.biometric.BiometricManager;

/** بيفحص إذا الجوال فيه بصمة (أو وجه) جاهزة ومسجّلة، قبل ما نعرض خيار البصمة. */
public final class BiometricHelper {

    /** BIOMETRIC_WEAK بيشمل أغلب حسّاسات البصمة والوجه على الجوالات. */
    public static final int AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK;

    private BiometricHelper() {}

    public static boolean canUse(Context context) {
        return BiometricManager.from(context).canAuthenticate(AUTHENTICATORS)
                == BiometricManager.BIOMETRIC_SUCCESS;
    }
}
