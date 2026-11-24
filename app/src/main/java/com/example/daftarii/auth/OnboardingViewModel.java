package com.example.daftarii.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

/** منطق شاشة الترحيب: بيحفظ إنه المستخدم شافها، عشان ما ترجع تطلع. */
public class OnboardingViewModel extends AndroidViewModel {

    private final SessionPrefs prefs;

    public OnboardingViewModel(@NonNull Application app) {
        super(app);
        prefs = new SessionPrefs(app);
    }

    /** لما يضغط "تخطي" أو "ابدأ الآن". */
    public void complete() {
        prefs.setOnboardingDone(true);
    }
}
