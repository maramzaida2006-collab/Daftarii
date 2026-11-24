package com.example.daftarii.logic;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * حالة الزبون، بتنحسب تلقائياً من الرصيد وموعد السداد.
 * الوقت الحالي بينبعت كـ parameter عشان نقدر نختبرها بـ JUnit.
 *
 * الترتيب:
 * 1. الرصيد صفر أو أقل: مسدّد.
 * 2. الموعد فات: متأخر X أيام.
 * 3. الموعد اليوم أو خلال 3 أيام: يستحق قريباً.
 * 4. غير هيك (أو ما في موعد): منتظم.
 */
public final class CustomerStatus {

    public static final int DUE_SOON_DAYS = 3;

    public enum Type { SETTLED, OVERDUE, DUE_SOON, REGULAR }

    private final Type type;
    /** للمتأخر: كم يوم فات. ليستحق قريباً: كم يوم ضايل (0 = اليوم). غير هيك 0. */
    private final int days;

    private CustomerStatus(Type type, int days) {
        this.type = type;
        this.days = days;
    }

    public Type getType() { return type; }
    public int getDays() { return days; }

    /**
     * @param balance     الرصيد بالأغورة
     * @param dueDateMillis موعد السداد، أو null/0 إذا ما في موعد
     * @param nowMillis   الوقت الحالي (System.currentTimeMillis() بالتطبيق)
     * @param timeZone    توقيت الجهاز (TimeZone.getDefault() بالتطبيق)
     */
    public static CustomerStatus evaluate(long balance, Long dueDateMillis,
                                          long nowMillis, TimeZone timeZone) {
        if (balance <= 0) return new CustomerStatus(Type.SETTLED, 0);
        if (dueDateMillis == null || dueDateMillis <= 0) {
            return new CustomerStatus(Type.REGULAR, 0);
        }
        int daysUntilDue = daysBetween(nowMillis, dueDateMillis, timeZone);
        if (daysUntilDue < 0) return new CustomerStatus(Type.OVERDUE, -daysUntilDue);
        if (daysUntilDue <= DUE_SOON_DAYS) return new CustomerStatus(Type.DUE_SOON, daysUntilDue);
        return new CustomerStatus(Type.REGULAR, 0);
    }

    /** فرق الأيام بالتقويم (مش بالساعات) من from لـ to. */
    static int daysBetween(long fromMillis, long toMillis, TimeZone tz) {
        long from = startOfDay(fromMillis, tz);
        long to = startOfDay(toMillis, tz);
        // Math.round عشان التوقيت الصيفي (يوم فيه 23 أو 25 ساعة)
        return (int) Math.round((to - from) / 86_400_000.0);
    }

    private static long startOfDay(long millis, TimeZone tz) {
        Calendar c = Calendar.getInstance(tz);
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
