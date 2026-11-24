package com.example.daftarii.auth;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.daftarii.logic.AuthError;
import com.example.daftarii.logic.Validators;
import com.example.daftarii.util.Event;

/** منطق شاشة "نسيت كلمة السر". */
public class ForgotPasswordViewModel extends ViewModel {

    private final AuthRepository repo = new AuthRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> emailError = new MutableLiveData<>();
    /** true بعد ما ينبعت الرابط: الشاشة بتظهر بطاقة "تم الإرسال". */
    private final MutableLiveData<Boolean> sent = new MutableLiveData<>(false);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Integer> getEmailError() { return emailError; }
    public LiveData<Boolean> getSent() { return sent; }
    public LiveData<Event<Integer>> getMessage() { return message; }

    public void send(String email) {
        Integer err = AuthMessages.emailError(Validators.email(email));
        emailError.setValue(err);
        if (err != null) return;

        loading.setValue(true);
        sent.setValue(false);
        repo.sendPasswordReset(email, new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                loading.setValue(false);
                sent.setValue(true);
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                if (error == AuthError.INVALID_EMAIL) {
                    emailError.setValue(AuthMessages.of(error));
                } else if (error == AuthError.WRONG_CREDENTIALS) {
                    // بعض نسخ Firebase بترجع "مش موجود": ما بنحكيله، بنعرض نفس رسالة النجاح
                    sent.setValue(true);
                } else {
                    message.setValue(new Event<>(AuthMessages.of(error)));
                }
            }
        });
    }
}
