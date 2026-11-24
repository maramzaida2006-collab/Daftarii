package com.example.daftarii.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.daftarii.logic.GuestTrial;
import com.example.daftarii.logic.StartRouter;

/**
 * منطق شاشة البداية: بيجمع حالة المستخدم ومن StartRouter بيعرف وين يروح.
 * القرار نفسه بـ StartRouter (منطق نقي ومفحوص بـ JUnit)، وهون بس بنجيب المعلومات.
 */
public class SplashViewModel extends AndroidViewModel {

    private final AuthRepository repo;
    private final SessionPrefs prefs;

    public SplashViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository();
        prefs = new SessionPrefs(app);
    }

    /**
     * بيرجّع وين لازم يروح المستخدم. إذا الجواب LOGIN لمستخدم مسجّل
     * (حساب مفعّل بس ما علّم "تذكرني")، بيطلّعه أول.
     */
    @NonNull
    public StartRouter.Destination decideStart() {
        boolean signedIn = repo.isSignedIn();
        boolean guest = repo.isGuest();
        boolean expired = guest
                && GuestTrial.isExpired(repo.accountCreatedAt(), System.currentTimeMillis());

        StartRouter.Destination destination = StartRouter.decide(
                prefs.isOnboardingDone(), signedIn, guest,
                repo.isEmailVerified(), prefs.isRememberMe(), expired);

        if (StartRouter.shouldSignOut(destination, signedIn)) {
            repo.signOut();
        }
        return destination;
    }
}
