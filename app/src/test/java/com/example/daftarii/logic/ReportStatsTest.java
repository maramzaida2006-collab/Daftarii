package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public class ReportStatsTest {

    private static final long DAY = 24L * 60 * 60 * 1000;

    private static long date(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, day, 10, 0, 0);
        return c.getTimeInMillis();
    }

    /** 26 نوفمبر 2026. */
    private static final long NOW = date(2026, Calendar.NOVEMBER, 26);

    private static ReportStats.Tx debt(long a, long d) {
        return new ReportStats.Tx(TransactionType.DEBT, a, d);
    }

    private static ReportStats.Tx pay(long a, long d) {
        return new ReportStats.Tx(TransactionType.PAYMENT, a, d);
    }

    private final List<ReportStats.Tx> txs = Arrays.asList(
            debt(5000, date(2026, Calendar.NOVEMBER, 3)),
            debt(2000, date(2026, Calendar.NOVEMBER, 20)),
            pay(3000, date(2026, Calendar.NOVEMBER, 21)),
            debt(10000, date(2026, Calendar.OCTOBER, 10)),
            pay(4000, date(2026, Calendar.SEPTEMBER, 15)),
            debt(9900, date(2026, Calendar.MARCH, 1)),
            debt(7700, date(2025, Calendar.DECEMBER, 30)));

    @Test
    public void thisMonth_countsOnlyNovember() {
        ReportStats r = ReportStats.compute(txs, Collections.emptyList(), ReportStats.Period.THIS_MONTH, NOW);
        assertEquals(7000, r.newDebts);
        assertEquals(2, r.newDebtsCount);
        assertEquals(3000, r.collected);
        assertEquals(1, r.collectedCount);
        assertEquals(43, r.percent); // 3000 / 7000
    }

    @Test
    public void threeMonths_isSepToNov() {
        ReportStats r = ReportStats.compute(txs, Collections.emptyList(), ReportStats.Period.THREE_MONTHS, NOW);
        assertEquals(17000, r.newDebts);
        assertEquals(7000, r.collected);
        assertEquals(2, r.collectedCount);
    }

    @Test
    public void thisYear_excludesLastYear() {
        ReportStats r = ReportStats.compute(txs, Collections.emptyList(), ReportStats.Period.THIS_YEAR, NOW);
        assertEquals(26900, r.newDebts);
        assertEquals(4, r.newDebtsCount);
    }

    @Test
    public void percent_cappedAt100_andZeroWhenEmpty() {
        List<ReportStats.Tx> over = Arrays.asList(debt(1000, NOW - DAY), pay(5000, NOW - DAY));
        assertEquals(100, ReportStats.compute(over, Collections.emptyList(), ReportStats.Period.THIS_MONTH, NOW).percent);
        assertEquals(0, ReportStats.compute(Collections.emptyList(), Collections.emptyList(),
                ReportStats.Period.THIS_MONTH, NOW).percent);
        List<ReportStats.Tx> onlyPay = Collections.singletonList(pay(500, NOW - DAY));
        assertEquals(100, ReportStats.compute(onlyPay, Collections.emptyList(), ReportStats.Period.THIS_MONTH, NOW).percent);
    }

    @Test
    public void overdue_andNewCustomers() {
        List<ReportStats.Cust> cs = Arrays.asList(
                new ReportStats.Cust(4000, NOW - DAY, date(2026, Calendar.NOVEMBER, 2)),   // متأخر + جديد
                new ReportStats.Cust(0, NOW - DAY, date(2026, Calendar.OCTOBER, 2)),       // رصيده صفر: مش متأخر
                new ReportStats.Cust(2500, NOW + DAY, date(2026, Calendar.NOVEMBER, 25)),  // جديد، مش متأخر
                new ReportStats.Cust(1500, NOW - 3 * DAY, 0));                             // متأخر، تاريخ مش معروف
        ReportStats r = ReportStats.compute(Collections.emptyList(), cs, ReportStats.Period.THIS_MONTH, NOW);
        assertEquals(2, r.overdueCount);
        assertEquals(5500, r.overdueTotal);
        assertEquals(2, r.newCustomers);
        assertEquals(3, ReportStats.compute(Collections.emptyList(), cs, ReportStats.Period.THREE_MONTHS, NOW).newCustomers);
    }

    @Test
    public void monthly_sixBuckets_oldestFirst() {
        long[][] m = ReportStats.monthly(txs, NOW, 6); // يونيو..نوفمبر
        assertEquals(6, m.length);
        assertEquals(7000, m[5][0]);
        assertEquals(3000, m[5][1]);
        assertEquals(10000, m[4][0]);
        assertEquals(4000, m[3][1]);
        assertEquals(0, m[0][0]);
    }
}
