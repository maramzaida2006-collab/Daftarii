package com.example.daftarii.auth;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.daftarii.R;
import com.example.daftarii.logic.Validators;
import com.example.daftarii.util.Event;

/**
 * إنشاء PIN بخطوتين: بيكتبه، وبعدين بيأكده. ومعه خيار تفعيل البصمة
 * (بيبيّن بس إذا الجوال فيه بصمة جاهزة). الـ PIN بينحفظ مربوط بالـ uid.
 */
public class CreatePinViewModel extends AndroidViewModel {

    public enum Nav { DONE, BACK_TO_LOGIN, CLOSE, CHANGED }

    /** شوي وقت بعد آخر رقم، عشان المستخدم يشوف النقطة الرابعة تتعبّى. */
    private static final long STEP_DELAY_MS = 180;

    private final AuthRepository repo;
    private final PinStore store;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final boolean biometricAvailable;

    private final StringBuilder input = new StringBuilder();
    private String firstPin;
    private boolean busy;
    /** تغيير PIN من حسابي: الرجوع بيسكّر الشاشة (ما بيطلّعه من الحساب)، والحفظ بيرجعه لحسابي. */
    private boolean changeMode;

    private final MutableLiveData<Integer> step = new MutableLiveData<>(1);
    private final MutableLiveData<Integer> filled = new MutableLiveData<>(0);
    private final MutableLiveData<Boolean> biometricOn;
    private final MutableLiveData<Event<Integer>> error = new MutableLiveData<>();
    private final MutableLiveData<Event<Nav>> navigation = new MutableLiveData<>();

    public CreatePinViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository();
        store = new PinStore(app);
        biometricAvailable = BiometricHelper.canUse(app);
        // إذا متاحة، بتبلّش مفعّلة (متل التصميم)، والمستخدم بيقدر يطفيها
        biometricOn = new MutableLiveData<>(biometricAvailable);
    }

    public LiveData<Integer> getStep() { return step; }
    public LiveData<Integer> getFilled() { return filled; }
    public LiveData<Boolean> getBiometricOn() { return biometricOn; }
    public LiveData<Event<Integer>> getError() { return error; }
    public LiveData<Event<Nav>> getNavigation() { return navigation; }

    public boolean isBiometricAvailable() { return biometricAvailable; }

    public boolean isSignedIn() { return repo.currentUid() != null; }

    public void setBiometricOn(boolean on) {
        biometricOn.setValue(biometricAvailable && on);
    }

    public void onDigit(char digit) {
        if (busy || input.length() >= Validators.PIN_LENGTH) return;
        input.append(digit);
        filled.setValue(input.length());
        if (input.length() == Validators.PIN_LENGTH) {
            busy = true;
            handler.postDelayed(this::onComplete, STEP_DELAY_MS);
        }
    }

    public void onDelete() {
        if (busy || input.length() == 0) return;
        input.deleteCharAt(input.length() - 1);
        filled.setValue(input.length());
    }

    /**
     * زر الرجوع: من خطوة التأكيد بيرجع للخطوة الأولى.
     * من الخطوة الأولى بيطلّع المستخدم (ما في دخول بدون PIN) وبيرجعه على تسجيل الدخول.
     */
    public void setChangeMode(boolean changeMode) {
        this.changeMode = changeMode;
    }

    public void onBack() {
        handler.removeCallbacksAndMessages(null);
        busy = false;
        if (Integer.valueOf(2).equals(step.getValue())) {
            resetTo(1);
        } else if (changeMode) {
            navigation.setValue(new Event<>(Nav.CLOSE));
        } else {
            repo.signOut();
            navigation.setValue(new Event<>(Nav.BACK_TO_LOGIN));
        }
    }

    private void onComplete() {
        busy = false;
        String pin = input.toString();
        if (Validators.pin(pin) != Validators.Result.OK) {
            resetTo(Integer.valueOf(2).equals(step.getValue()) ? 2 : 1);
            return;
        }
        if (!Integer.valueOf(2).equals(step.getValue())) {
            firstPin = pin;
            resetTo(2);
            return;
        }
        if (!pin.equals(firstPin)) {
            // التأكيد مش متطابق: من الأول
            error.setValue(new Event<>(R.string.pin_mismatch));
            resetTo(1);
            return;
        }
        String uid = repo.currentUid();
        if (uid == null) {
            navigation.setValue(new Event<>(Nav.BACK_TO_LOGIN));
            return;
        }
        store.savePin(uid, pin);
        store.setBiometricEnabled(uid, Boolean.TRUE.equals(biometricOn.getValue()));
        input.setLength(0);
        firstPin = null;
        navigation.setValue(new Event<>(changeMode ? Nav.CHANGED : Nav.DONE));
    }

    private void resetTo(int newStep) {
        input.setLength(0);
        if (newStep == 1) firstPin = null;
        step.setValue(newStep);
        filled.setValue(0);
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
    }
}
