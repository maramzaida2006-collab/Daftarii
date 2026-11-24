package com.example.daftarii.logic;

/**
 * حد المحاولات الغلط بشاشة القفل: بعد 5 محاولات غلط ورا بعض، بنوقف 30 ثانية.
 * وكل 5 غلطات بعدها بتضاعف المدة (30، 60، 120...) لحد 15 دقيقة.
 * الـ PIN بنجاح بيصفّر العدّاد. بدون Android، فبينفحص بـ JUnit.
 */
public final class PinAttempts {

    public static final int MAX_ATTEMPTS = 5;
    public static final long BASE_LOCKOUT_MS = 30_000L;
    public static final long MAX_LOCKOUT_MS = 15 * 60_000L;

    /** الحالة المحفوظة: كم غلطة ورا بعض، ولمتى ممنوع يجرّب (0 = مش ممنوع). */
    public static final class State {
        public final int failures;
        public final long lockedUntil;

        public State(int failures, long lockedUntil) {
            this.failures = Math.max(0, failures);
            this.lockedUntil = Math.max(0, lockedUntil);
        }

        public static State fresh() {
            return new State(0, 0);
        }
    }

    private PinAttempts() {}

    public static boolean isLocked(State state, long now) {
        return state.lockedUntil > now;
    }

    /** كم ثانية ضايلة على الوقف (مدوّرة لفوق). 0 إذا مش موقوف. */
    public static int secondsLeft(State state, long now) {
        long left = state.lockedUntil - now;
        return left <= 0 ? 0 : (int) ((left + 999) / 1000);
    }

    /** كم محاولة ضايلة قبل الوقف الجاي. */
    public static int attemptsLeft(State state) {
        int inRound = state.failures % MAX_ATTEMPTS;
        return MAX_ATTEMPTS - inRound;
    }

    /** محاولة غلط. كل ما يكمّل 5 غلطات، بيتوقف مدة أطول من المرة اللي قبلها. */
    public static State onFailure(State state, long now) {
        int failures = state.failures + 1;
        if (failures % MAX_ATTEMPTS != 0) {
            return new State(failures, state.lockedUntil);
        }
        int round = failures / MAX_ATTEMPTS;          // 1، 2، 3...
        long lockout = BASE_LOCKOUT_MS;
        for (int i = 1; i < round && lockout < MAX_LOCKOUT_MS; i++) lockout *= 2;
        lockout = Math.min(lockout, MAX_LOCKOUT_MS);
        return new State(failures, now + lockout);
    }

    /** الـ PIN صح أو البصمة نجحت: من أول وجديد. */
    public static State onSuccess() {
        return State.fresh();
    }
}
