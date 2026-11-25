package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public class DueAlertsTest {

    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Hebron");
    private static final long DAY = 24L * 60 * 60 * 1000;

    /** 25 نوفمبر 2026، الساعة 10 الصبح بتوقيت فلسطين. */
    private static long now() {
        Calendar c = Calendar.getInstance(TZ);
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 25, 10, 0, 0);
        return c.getTimeInMillis();
    }

    private static DueAlerts.CustomerInfo customer(String id, long balance, long due) {
        return new DueAlerts.CustomerInfo(id, "زبون " + id, "0591234567", balance, due);
    }

    @Test
    public void fromCustomers_picksTomorrowTodayOverdue() {
        long now = now();
        List<DueAlerts.Item> items = DueAlerts.fromCustomers(Arrays.asList(
                customer("a", 5000, now + DAY),          // بكرة
                customer("b", 5000, now + 2 * 3600_000), // اليوم
                customer("c", 5000, now - 5 * DAY),      // متأخر
                customer("d", 5000, now + 3 * DAY)       // بعد 3 أيام: لا
        ), now, TZ);
        assertEquals(3, items.size());
        assertEquals(DueAlerts.Type.DUE_TOMORROW, items.get(0).type);
        assertEquals(DueAlerts.Type.DUE_TODAY, items.get(1).type);
        assertEquals(DueAlerts.Type.OVERDUE, items.get(2).type);
        assertFalse(items.get(0).read);
    }

    @Test
    public void fromCustomers_skipsSettledAndNoDueDate() {
        long now = now();
        List<DueAlerts.Item> items = DueAlerts.fromCustomers(Arrays.asList(
                customer("a", 0, now - DAY),      // مسدّد
                customer("b", -100, now - DAY),   // رصيد للزبون
                customer("c", 5000, 0)            // بدون موعد
        ), now, TZ);
        assertTrue(items.isEmpty());
        assertTrue(DueAlerts.fromCustomers(null, now, TZ).isEmpty());
    }

    @Test
    public void merge_sameAlertTwice_addedOnce_andKeepsReadState() {
        long now = now();
        List<DueAlerts.Item> fresh = DueAlerts.fromCustomers(
                Collections.singletonList(customer("c", 5000, now - DAY)), now, TZ);
        List<DueAlerts.Item> saved = DueAlerts.merge(null, fresh, now);
        saved = DueAlerts.markAllRead(saved);

        // ساعة بعدين نفس الفحص
        List<DueAlerts.Item> again = DueAlerts.fromCustomers(
                Collections.singletonList(customer("c", 5000, now - DAY)), now + 3600_000, TZ);
        List<DueAlerts.Item> merged = DueAlerts.merge(saved, again, now + 3600_000);
        assertEquals(1, merged.size());
        assertTrue(merged.get(0).read);
        assertEquals(0, DueAlerts.unreadCount(merged));
        assertTrue(DueAlerts.added(saved, merged).isEmpty());
    }

    @Test
    public void merge_newDueDate_isNewAlert() {
        long now = now();
        List<DueAlerts.Item> saved = DueAlerts.markAllRead(DueAlerts.merge(null,
                DueAlerts.fromCustomers(Collections.singletonList(customer("c", 5000, now - DAY)), now, TZ), now));
        // الزبون أخد موعد جديد وتأخر عنه كمان
        List<DueAlerts.Item> fresh = DueAlerts.fromCustomers(
                Collections.singletonList(customer("c", 5000, now - 2 * 3600_000 - DAY + 1)), now, TZ);
        List<DueAlerts.Item> merged = DueAlerts.merge(saved, fresh, now);
        assertEquals(2, merged.size());
        assertEquals(1, DueAlerts.unreadCount(merged));
        assertEquals(1, DueAlerts.added(saved, merged).size());
    }

    @Test
    public void merge_dropsOldAndSortsNewestFirst() {
        long now = now();
        DueAlerts.Item old = new DueAlerts.Item("old", DueAlerts.Type.OVERDUE, "x", "x", "",
                100, now - 40 * DAY, now - 31 * DAY, false);
        DueAlerts.Item mid = new DueAlerts.Item("mid", DueAlerts.Type.OVERDUE, "y", "y", "",
                100, now - 3 * DAY, now - 2 * DAY, false);
        DueAlerts.Item fresh = new DueAlerts.Item("new", DueAlerts.Type.DUE_TODAY, "z", "z", "",
                100, now, 0, false);
        List<DueAlerts.Item> merged = DueAlerts.merge(Arrays.asList(old, mid),
                Collections.singletonList(fresh), now);
        assertEquals(2, merged.size());
        assertEquals("new", merged.get(0).key);
        assertEquals(now, merged.get(0).createdAt);
        assertEquals("mid", merged.get(1).key);
    }

    @Test
    public void merge_capsAtMax() {
        long now = now();
        List<DueAlerts.Item> many = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            many.add(new DueAlerts.Item("k" + i, DueAlerts.Type.OVERDUE, "c" + i, "", "", 1, 1, now - i, false));
        }
        assertEquals(DueAlerts.MAX_ITEMS, DueAlerts.merge(many, null, now).size());
    }

    @Test
    public void markRead_onlyThatOne() {
        long now = now();
        List<DueAlerts.Item> items = DueAlerts.merge(null, DueAlerts.fromCustomers(Arrays.asList(
                customer("a", 5000, now - DAY), customer("b", 5000, now + DAY)), now, TZ), now);
        List<DueAlerts.Item> after = DueAlerts.markRead(items, items.get(0).key);
        assertEquals(1, DueAlerts.unreadCount(after));
    }

    @Test
    public void trialEnding_onlyLastTwoDays() {
        long now = now();
        assertNull(DueAlerts.trialEnding("u", 3, now));
        assertNull(DueAlerts.trialEnding("u", 0, now));
        assertNull(DueAlerts.trialEnding(null, 1, now));
        DueAlerts.Item two = DueAlerts.trialEnding("u", 2, now);
        DueAlerts.Item one = DueAlerts.trialEnding("u", 1, now);
        assertEquals(DueAlerts.Type.TRIAL_ENDING, one.type);
        assertFalse(one.isPayment());
        assertFalse(two.key.equals(one.key));
    }

    @Test
    public void filter_paymentsAndSystem() {
        long now = now();
        List<DueAlerts.Item> items = new ArrayList<>(DueAlerts.fromCustomers(
                Collections.singletonList(customer("a", 5000, now - DAY)), now, TZ));
        items.add(DueAlerts.trialEnding("u", 1, now));
        assertEquals(2, DueAlerts.filter(items, null).size());
        assertEquals(1, DueAlerts.filter(items, true).size());
        assertEquals(1, DueAlerts.filter(items, false).size());
    }

    @Test
    public void daysLate_countsCalendarDays() {
        long now = now();
        assertEquals(5, DueAlerts.daysLate(now - 5 * DAY, now, TZ));
        assertEquals(0, DueAlerts.daysLate(now + DAY, now, TZ));
    }

    @Test
    public void whatsAppNumber_palestinianAndOther() {
        assertEquals("970591234567", DueAlerts.whatsAppNumber("0591234567"));
        assertEquals("970561234567", DueAlerts.whatsAppNumber("056-123 4567"));
        assertEquals("970591234567", DueAlerts.whatsAppNumber("+970 59 123 4567"));
        assertEquals("972521234567", DueAlerts.whatsAppNumber("0521234567"));
        assertEquals("970591234567", DueAlerts.whatsAppNumber("٠٥٩١٢٣٤٥٦٧"));
        assertEquals("", DueAlerts.whatsAppNumber(""));
        assertEquals("", DueAlerts.whatsAppNumber(null));
        assertEquals("", DueAlerts.whatsAppNumber("123"));
    }
}
