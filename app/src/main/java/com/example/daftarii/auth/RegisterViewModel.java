package com.example.daftarii.auth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.daftarii.logic.AuthError;
import com.example.daftarii.logic.PasswordStrength;
import com.example.daftarii.logic.Validators;
import com.example.daftarii.util.Event;

/** منطق شاشة إنشاء حساب. */
public class RegisterViewModel extends AndroidViewModel {

    private final AuthRepository repo;
    private final SessionPrefs prefs;

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> shopNameError = new MutableLiveData<>();
    private final MutableLiveData<Integer> emailError = new MutableLiveData<>();
    private final MutableLiveData<Integer> passwordError = new MutableLiveData<>();
    private final MutableLiveData<Integer> confirmError = new MutableLiveData<>();
    private final MutableLiveData<PasswordStrength.Level> strength =
            new MutableLiveData<>(PasswordStrength.Level.NONE);
    private final MutableLiveData<Integer> strengthScore = new MutableLiveData<>(0);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Boolean>> registered = new MutableLiveData<>();

    public RegisterViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository();
        prefs = new SessionPrefs(app);
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Integer> getShopNameError() { return shopNameError; }
    public LiveData<Integer> getEmailError() { return emailError; }
    public LiveData<Integer> getPasswordError() { return passwordError; }
    public LiveData<Integer> getConfirmError() { return confirmError; }
    public LiveData<PasswordStrength.Level> getStrength() { return strength; }
    public LiveData<Integer> getStrengthScore() { return strengthScore; }
    public LiveData<Event<Integer>> getMessage() { return message; }
    /** لما يصير true: الحساب انعمل والرابط انبعت، روح لشاشة التفعيل. */
    public LiveData<Event<Boolean>> getRegistered() { return registered; }

    /** بتنادى مع كل حرف بحقل كلمة السر عشان المؤشر يتحدّث. */
    public void onPasswordChanged(String password) {
        strength.setValue(PasswordStrength.level(password));
        strengthScore.setValue(password == null || password.isEmpty() ? 0 : PasswordStrength.score(password));
    }

    public void register(String shopName, String email, String password, String confirmation) {
        Integer nErr = AuthMessages.shopNameError(Validators.shopName(shopName));
        Integer eErr = AuthMessages.emailError(Validators.email(email));
        Integer pErr = AuthMessages.passwordError(Validators.password(password));
        Integer cErr = AuthMessages.confirmationError(Validators.passwordConfirmation(password, confirmation));
        shopNameError.setValue(nErr);
        emailError.setValue(eErr);
        passwordError.setValue(pErr);
        confirmError.setValue(cErr);
        if (nErr != null || eErr != null || pErr != null || cErr != null) return;

        loading.setValue(true);
        repo.register(shopName, email, password, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                loading.setValue(false);
                // اللي عمل حساب جديد بدّه يضل داخل بعد التفعيل
                prefs.setRememberMe(true);
                registered.setValue(new Event<>(true));
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                if (error == AuthError.INVALID_EMAIL) {
                    emailError.setValue(AuthMessages.of(error));
                } else if (error == AuthError.EMAIL_IN_USE) {
                    emailError.setValue(AuthMessages.of(error));
                } else if (error == AuthError.WEAK_PASSWORD) {
                    passwordError.setValue(AuthMessages.of(error));
                } else {
                    message.setValue(new Event<>(AuthMessages.of(error)));
                }
            }
        });
    }
}
