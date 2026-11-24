package com.example.daftarii.auth;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.daftarii.logic.PinAttempts;
import com.example.daftarii.logic.Validators;
import com.example.daftarii.util.Event;

/**
 * شاشة القفل: PIN من 4 أرقام أو البصمة. بعد 5 محاولات غلط بيتوقف مدة (PinAttempts).
 * "الدخول بحساب آخر" بيمسح الـ PIN وبيطلّع المستخدم.
 */
public class LockViewModel extends AndroidViewModel {

    public enum Nav { UNLOCKED, CREATE_PIN, LOGIN }

    /** رسالة تحت النقاط. */
    public static final class Message {
        public final boolean wrongPin;     // true: "الرمز غلط"، false: موقوف
        public final int value;            // المحاولات الضايلة، أو الثواني الضايلة

        Message(boolean wrongPin, int value) {
            this.wrongPin = wrongPin;
            this.value = value;
        }
    }

    private static final long CHECK_DELAY_MS = 150;

    private final AuthRepository repo;
    private final PinStore store;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final StringBuilder input = new StringBuilder();
    private boolean busy;

    private final MutableLiveData<Integer> filled = new MutableLiveData<>(0);
    /** null: ما في رسالة. */
    private final MutableLiveData<Message> message = new MutableLiveData<>();
    private final MutableLiveData<Boolean> locked = new MutableLiveData<>(false);
    /** بتنبعت مع كل غلطة عشان الشاشة تهزّ النقاط. */
    private final MutableLiveData<Event<Boolean>> shake = new MutableLiveData<>();
    private final MutableLiveData<Event<Nav>> navigation = new MutableLiveData<>();

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            refreshLockout();
        }
    };

    public LockViewModel(@NonNull Application app) {
        super(app);
        repo = new AuthRepository();
        store = new PinStore(app);
    }

    public LiveData<Integer> getFilled() { return filled; }
    public LiveData<Message> getMessage() { return message; }
    public LiveData<Boolean> getLocked() { return locked; }
    public LiveData<Event<Boolean>> getShake() { return shake; }
    public LiveData<Event<Nav>> getNavigation() { return navigation; }

    /** لما الشاشة تفتح: إذا ما في مستخدم أو ما في PIN، بنوجّهه. */
    public void start() {
        String uid = repo.currentUid();
        if (uid == null || repo.isGuest()) {
            navigation.setValue(new Event<>(Nav.LOGIN));
            return;
        }
        if (!store.hasPin(uid)) {
            navigation.setValue(new Event<>(Nav.CREATE_PIN));
            return;
        }
        refreshLockout();
    }

    @Nullable
    public String shopName() {
        return repo.currentDisplayName();
    }

    /** البصمة بتبيّن إذا المستخدم فعّلها والجوال لسا فيه بصمة جاهزة. */
    public boolean isBiometricEnabled() {
        String uid = repo.currentUid();
        return uid != null && store.isBiometricEnabled(uid) && BiometricHelper.canUse(getApplication());
    }

    public void onDigit(char digit) {
        if (busy || Boolean.TRUE.equals(locked.getValue())
                || input.length() >= Validators.PIN_LENGTH) return;
        input.append(digit);
        filled.setValue(input.length());
        if (input.length() == Validators.PIN_LENGTH) {
            busy = true;
            handler.postDelayed(this::check, CHECK_DELAY_MS);
        }
    }

    public void onDelete() {
        if (busy || input.length() == 0) return;
        input.deleteCharAt(input.length() - 1);
        filled.setValue(input.length());
    }

    public void onBiometricSuccess() {
        String uid = repo.currentUid();
        if (uid == null) return;
        store.saveAttempts(uid, PinAttempts.onSuccess());
        navigation.setValue(new Event<>(Nav.UNLOCKED));
    }

    /** "الدخول بحساب آخر": بيمسح الـ PIN وبيطلع من الحساب. */
    public void useAnotherAccount() {
        store.clear();
        repo.signOut();
        navigation.setValue(new Event<>(Nav.LOGIN));
    }

    private void check() {
        busy = false;
        String uid = repo.currentUid();
        if (uid == null) {
            navigation.setValue(new Event<>(Nav.LOGIN));
            return;
        }
        String pin = input.toString();
        input.setLength(0);
        filled.setValue(0);

        if (store.checkPin(uid, pin)) {
            store.saveAttempts(uid, PinAttempts.onSuccess());
            message.setValue(null);
            navigation.setValue(new Event<>(Nav.UNLOCKED));
            return;
        }
        PinAttempts.State state = PinAttempts.onFailure(store.attempts(uid), System.currentTimeMillis());
        store.saveAttempts(uid, state);
        shake.setValue(new Event<>(true));
        if (PinAttempts.isLocked(state, System.currentTimeMillis())) {
            refreshLockout();
        } else {
            message.setValue(new Message(true, PinAttempts.attemptsLeft(state)));
        }
    }

    /** بيحدّث حالة الوقف، وإذا لسا موقوف بيعيد الفحص كل ثانية (عدّاد تنازلي). */
    private void refreshLockout() {
        handler.removeCallbacks(tick);
        String uid = repo.currentUid();
        if (uid == null) return;
        PinAttempts.State state = store.attempts(uid);
        long now = System.currentTimeMillis();
        if (PinAttempts.isLocked(state, now)) {
            locked.setValue(true);
            message.setValue(new Message(false, PinAttempts.secondsLeft(state, now)));
            handler.postDelayed(tick, 1000);
        } else {
            if (Boolean.TRUE.equals(locked.getValue())) message.setValue(null);
            locked.setValue(false);
        }
    }

    @Override
    protected void onCleared() {
        handler.removeCallbacksAndMessages(null);
    }
}
