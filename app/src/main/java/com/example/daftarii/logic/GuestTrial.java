package com.example.daftarii.logic;

/**
 * الفترة التجريبية للزائر: 7 أيام من أول دخول كزائر.
 * وقت البداية بييجي من FirebaseUser.getMetadata().getCreationTimestamp()،
 * فما بيتغيّر لو المستخدم غيّر ساعة الجوال.
 */
public final class GuestTrial {

    public static final int TRIAL_DAYS = 7;
    private static final long DAY_MILLIS = 86_400_000L;

    private GuestTrial() {}

    /**
     * كم يوم ضايل. اليوم الأول بيرجّع 7، وآخر يوم بيرجّع 1، وبعد ما تخلص بيرجّع 0.
     * إذا وقت البداية مش معروف (0 أو أقل) بنعتبره لسا بادئ.
     */
    public static int daysLeft(long startedAtMillis, long nowMillis) {
        if (startedAtMillis <= 0) return TRIAL_DAYS;
        long elapsed = Math.max(0, nowMillis - startedAtMillis);
        long remainingMillis = TRIAL_DAYS * DAY_MILLIS - elapsed;
        if (remainingMillis <= 0) return 0;
        // أي جزء من يوم بينحسب يوم كامل: لو ضايل 30 ساعة، بيرجّع 2
        return (int) ((remainingMillis + DAY_MILLIS - 1) / DAY_MILLIS);
    }

    public static boolean isExpired(long startedAtMillis, long nowMillis) {
        return daysLeft(startedAtMillis, nowMillis) == 0;
    }
}
