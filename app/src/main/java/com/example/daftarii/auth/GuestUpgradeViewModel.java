package com.example.daftarii.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.daftarii.logic.AuthError;
import com.example.daftarii.logic.GuestTrial;
import com.example.daftarii.logic.Validators;
import com.example.daftarii.util.Event;

/**
 * تحويل الزائر لحساب (اسم المحل، الإيميل، كلمة السر). البيانات بتضل لأنه الـ uid نفسه.
 * بعد النجاح: رابط تفعيل، وبعدين شاشة "فعّل بريدك" متل التسجيل العادي.
 */
public class GuestUpgradeViewModel extends AndroidViewModel {

    public enum Nav { VERIFY_EMAIL, LOGIN }

    private final AuthRepository repo;
    private final SessionPrefs prefs;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> shopNameError = new MutableLiveData<>();
    private final MutableLiveData<Integer> emailError = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordError = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Nav>> navigation = new MutableLiveData<>();

    public GuestUpgradeViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository();
        prefs = new SessionPrefs(app);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Integer> getShopNameError() { return shopNameError; }
    public LiveData<Integer> getEmailError() { return emailError; }
    public LiveData<Integer> getPasswordError() { return passwordError; }
    public LiveData<Event<Integer>> getMessage() { return message; }
    public LiveData<Event<Nav>> getNavigation() { return navigation; }

    /** الشاشة بتشتغل بس للزائر. */
    public boolean isGuest() {
        return repo.isGuest();
    }

    /** خلصت الـ 7 أيام: الشاشة ما بتنسكّر وما في رجوع للرئيسية. */
    public boolean isTrialExpired() {
        return repo.isGuest()
                && GuestTrial.isExpired(repo.accountCreatedAt(), System.currentTimeMillis());
    }

    public void upgrade(String shopName, String email, String password) {
        Integer nErr = AuthMessages.shopNameError(Validators.shopName(shopName));
        Integer eErr = AuthMessages.emailError(Validators.email(email));
        Integer pErr = AuthMessages.passwordError(Validators.password(password));
        shopNameError.setValue(nErr);
        emailError.setValue(eErr);
        passwordError.setValue(pErr);
        if (nErr != null || eErr != null || pErr != null) return;

        loading.setValue(true);
        repo.upgradeGuest(shopName, email, password, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                loading.setValue(false);
                prefs.setRememberMe(true);
                prefs.setUpgradedGuestPending(true);
                repo.saveUserProfile(shopName, false);
                navigation.setValue(new Event<>(Nav.VERIFY_EMAIL));
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                if (error == AuthError.INVALID_EMAIL || error == AuthError.EMAIL_IN_USE) {
                    emailError.setValue(AuthMessages.of(error));
                } else if (error == AuthError.WEAK_PASSWORD) {
                    passwordError.setValue(AuthMessages.of(error));
                } else {
                    message.setValue(new Event<>(AuthMessages.of(error)));
                }
            }
        });
    }

    /** "الدخول بحساب آخر": بيطلع من حساب الزائر (بياناته بتضل بس ما بيقدر يرجعلها). */
    public void leaveGuest() {
        repo.signOut();
        navigation.setValue(new Event<>(Nav.LOGIN));
    }
}
