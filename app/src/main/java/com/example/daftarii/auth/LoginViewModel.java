package com.example.daftarii.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.daftarii.R;
import com.example.daftarii.logic.AuthError;
import com.example.daftarii.logic.StartRouter;
import com.example.daftarii.logic.Validators;
import com.example.daftarii.util.Event;

/** منطق شاشة تسجيل الدخول. الشاشة بتسمع للـ LiveData وبتعرض بس. */
public class LoginViewModel extends AndroidViewModel {

    private final AuthRepository repo;
    private final SessionPrefs prefs;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> emailError = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordError = new MutableLiveData<>();
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<StartRouter.Destination>> navigation = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository();
        prefs = new SessionPrefs(app);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Integer> getEmailError() { return emailError; }
    public LiveData<Integer> getPasswordError() { return passwordError; }
    public LiveData<Event<Integer>> getMessage() { return message; }
    public LiveData<Event<StartRouter.Destination>> getNavigation() { return navigation; }

    public boolean isRememberMe() {
        return prefs.isRememberMe();
    }

    public void login(String email, String password, boolean rememberMe) {
        Integer eErr = AuthMessages.emailError(Validators.email(email));
        Integer pErr = password == null || password.isEmpty()
                ? Integer.valueOf(R.string.error_password_empty) : null;
        emailError.setValue(eErr);
        passwordError.setValue(pErr);
        if (eErr != null || pErr != null) return;

        loading.setValue(true);
        repo.login(email, password, new AuthRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean verified) {
                loading.setValue(false);
                prefs.setRememberMe(rememberMe);
                navigation.setValue(new Event<>(Boolean.TRUE.equals(verified)
                        ? StartRouter.Destination.LOCK
                        : StartRouter.Destination.VERIFY_EMAIL));
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                message.setValue(new Event<>(AuthMessages.of(error)));
            }
        });
    }

    public void loginAsGuest() {
        loading.setValue(true);
        repo.signInAsGuest(new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                loading.setValue(false);
                navigation.setValue(new Event<>(StartRouter.Destination.HOME_GUEST));
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                message.setValue(new Event<>(AuthMessages.of(error)));
            }
        });
    }
}
