package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.TimeZone;

import org.junit.Test;

public class CustomerStatusTest {

    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Hebron");

    /** تاريخ بتوقيت فلسطين. الشهر من 1 لـ 12. */
    private static long at(int year, int month, int day, int hour, int minute) {
        Calendar c = Calendar.getInstance(TZ);
        c.clear();
        c.set(year, month - 1, day, hour, minute);
        return c.getTimeInMillis();
    }

    private static final long NOW = at(2026, 11, 24, 10, 0);

    private static CustomerStatus eval(long balance, Long due) {
        return CustomerStatus.evaluate(balance, due, NOW, TZ);
    }

    @Test
    public void zeroBalance_isSettled_evenIfOverdue() {
        CustomerStatus s = eval(0, at(2026, 11, 1, 0, 0));
        assertEquals(CustomerStatus.Type.SETTLED, s.getType());
    }

    @Test
    public void negativeBalance_isSettled() {
        assertEquals(CustomerStatus.Type.SETTLED, eval(-500, null).getType());
    }

    @Test
    public void noDueDate_isRegular() {
        assertEquals(CustomerStatus.Type.REGULAR, eval(5000, null).getType());
        assertEquals(CustomerStatus.Type.REGULAR, eval(5000, 0L).getType());
    }

    @Test
    public void dueYesterday_isOverdueOneDay() {
        CustomerStatus s = eval(5000, at(2026, 11, 23, 23, 59));
        assertEquals(CustomerStatus.Type.OVERDUE, s.getType());
        assertEquals(1, s.getDays());
    }

    @Test
    public void dueTenDaysAgo_isOverdueTenDays() {
        CustomerStatus s = eval(5000, at(2026, 11, 14, 12, 0));
        assertEquals(CustomerStatus.Type.OVERDUE, s.getType());
        assertEquals(10, s.getDays());
    }

    @Test
    public void dueToday_isDueSoon_notOverdue() {
        // الموعد اليوم الساعة 8 الصبح، وهلأ 10: لسا ما فات اليوم
        CustomerStatus s = eval(5000, at(2026, 11, 24, 8, 0));
        assertEquals(CustomerStatus.Type.DUE_SOON, s.getType());
        assertEquals(0, s.getDays());
    }

    @Test
    public void dueInThreeDays_isDueSoon() {
        CustomerStatus s = eval(5000, at(2026, 11, 27, 0, 0));
        assertEquals(CustomerStatus.Type.DUE_SOON, s.getType());
        assertEquals(3, s.getDays());
    }

    @Test
    public void dueInFourDays_isRegular() {
        assertEquals(CustomerStatus.Type.REGULAR, eval(5000, at(2026, 11, 28, 0, 0)).getType());
    }

    @Test
    public void daysBetween_acrossDstChange() {
        // بفلسطين الساعة بترجع بآخر أكتوبر، فاليوم بيكون 25 ساعة
        long from = at(2026, 10, 20, 10, 0);
        long to = at(2026, 11, 3, 10, 0);
        assertEquals(14, CustomerStatus.daysBetween(from, to, TZ));
    }
}
