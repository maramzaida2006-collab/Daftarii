package com.example.daftarii.auth;

import android.os.CountDownTimer;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.daftarii.R;
import com.example.daftarii.logic.AuthError;
import com.example.daftarii.util.Event;

/** منطق شاشة "فعّل بريدك". */
public class VerifyEmailViewModel extends ViewModel {

    public enum Nav { VERIFIED, BACK_TO_LOGIN, BACK_TO_REGISTER }

    /** المدة قبل ما يقدر يعيد الإرسال، عشان Firebase ما يحظره (too many requests). */
    public static final int RESEND_COOLDOWN_SECONDS = 45;

    private final AuthRepository repo = new AuthRepository();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    /** الثواني الضايلة قبل ما يقدر يعيد الإرسال. 0 = بيقدر هلأ. */
    private final MutableLiveData<Integer> resendSeconds = new MutableLiveData<>(0);
    private final MutableLiveData<Event<Integer>> message = new MutableLiveData<>();
    private final MutableLiveData<Event<Nav>> navigation = new MutableLiveData<>();

    private CountDownTimer timer;

    public VerifyEmailViewModel() {
        // الرابط انبعت هلأ مع إنشاء الحساب، فبنبلّش العداد من أول
        startCooldown();
    }

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Integer> getResendSeconds() { return resendSeconds; }
    public LiveData<Event<Integer>> getMessage() { return message; }
    public LiveData<Event<Nav>> getNavigation() { return navigation; }

    public String getEmail() {
        String email = repo.currentEmail();
        return email != null ? email : "";
    }

    /** زر "فعّلت، تابع". */
    public void checkVerified() {
        loading.setValue(true);
        repo.reloadAndCheckVerified(new AuthRepository.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean verified) {
                loading.setValue(false);
                if (Boolean.TRUE.equals(verified)) {
                    navigation.setValue(new Event<>(Nav.VERIFIED));
                } else {
                    message.setValue(new Event<>(R.string.verify_not_yet));
                }
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                message.setValue(new Event<>(AuthMessages.of(error)));
            }
        });
    }

    public void resend() {
        Integer left = resendSeconds.getValue();
        if (left != null && left > 0) return;
        loading.setValue(true);
        repo.resendVerificationEmail(new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                loading.setValue(false);
                message.setValue(new Event<>(R.string.verify_resent));
                startCooldown();
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                message.setValue(new Event<>(AuthMessages.of(error)));
            }
        });
    }

    /** زر الرجوع: بيطلّع المستخدم، وبيقدر يسجّل دخول بعدين وبيرجع لهون إذا لسا ما فعّل. */
    public void back() {
        repo.signOut();
        navigation.setValue(new Event<>(Nav.BACK_TO_LOGIN));
    }

    /** "تغيير البريد الإلكتروني": بيمسح الحساب غير المفعّل وبيرجع لإنشاء حساب. */
    public void changeEmail() {
        loading.setValue(true);
        repo.deleteUnverifiedAccount(new AuthRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                loading.setValue(false);
                navigation.setValue(new Event<>(Nav.BACK_TO_REGISTER));
            }

            @Override
            public void onError(@NonNull AuthError error) {
                loading.setValue(false);
                navigation.setValue(new Event<>(Nav.BACK_TO_REGISTER));
            }
        });
    }

    private void startCooldown() {
        if (timer != null) timer.cancel();
        resendSeconds.setValue(RESEND_COOLDOWN_SECONDS);
        timer = new CountDownTimer(RESEND_COOLDOWN_SECONDS * 1000L, 1000L) {
            @Override
            public void onTick(long millisUntilFinished) {
                resendSeconds.setValue((int) Math.ceil(millisUntilFinished / 1000.0));
            }

            @Override
            public void onFinish() {
                resendSeconds.setValue(0);
            }
        };
        timer.start();
    }

    @Override
    protected void onCleared() {
        if (timer != null) timer.cancel();
    }
}
