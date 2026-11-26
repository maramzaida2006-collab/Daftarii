package com.example.daftarii.logic;

import java.util.Calendar;
import java.util.List;

/**
 * حسابات شاشة التقارير (بدون أندرويد، عشان تنفحص بـ JUnit).
 * كل المبالغ بالأغورة (long).
 */
public final class ReportStats {

    public enum Period { THIS_MONTH, THREE_MONTHS, THIS_YEAR }

    /** حركة: النوع والمبلغ (موجب دايماً) والتاريخ. */
    public static final class Tx {
        final TransactionType type;
        final long amount;
        final long date;

        public Tx(TransactionType type, long amount, long date) {
            this.type = type;
            this.amount = amount;
            this.date = date;
        }
    }

    /** زبون: رصيده وموعد الاستحقاق وتاريخ الإضافة. */
    public static final class Cust {
        final long balance;
        final long due;
        final long created;

        public Cust(long balance, long due, long created) {
            this.balance = balance;
            this.due = due;
            this.created = created;
        }
    }

    public long newDebts;
    public int newDebtsCount;
    public long collected;
    public int collectedCount;
    public int overdueCount;
    public long overdueTotal;
    public int newCustomers;
    /** نسبة التحصيل 0..100 = المحصّل ÷ الديون الجديدة بنفس الفترة. */
    public int percent;

    /** أول يوم بالشهر قبل monthsBack شهر (0 = هالشهر)، الساعة 00:00. */
    public static long monthStart(long now, int monthsBack) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(now);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        c.add(Calendar.MONTH, -monthsBack);
        return c.getTimeInMillis();
    }

    /** بداية الفترة: هالشهر، أو آخر 3 أشهر (هالشهر + الشهرين اللي قبله)، أو من أول السنة. */
    public static long periodStart(Period period, long now) {
        switch (period) {
            case THREE_MONTHS:
                return monthStart(now, 2);
            case THIS_YEAR: {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(monthStart(now, 0));
                c.set(Calendar.MONTH, Calendar.JANUARY);
                return c.getTimeInMillis();
            }
            case THIS_MONTH:
            default:
                return monthStart(now, 0);
        }
    }

    public static ReportStats compute(List<Tx> txs, List<Cust> customers, Period period, long now) {
        ReportStats r = new ReportStats();
        long start = periodStart(period, now);

        for (Tx t : txs) {
            if (t.date < start || t.date > now) continue;
            if (t.type == TransactionType.PAYMENT) {
                r.collected += t.amount;
                r.collectedCount++;
            } else {
                r.newDebts += t.amount;
                r.newDebtsCount++;
            }
        }

        for (Cust c : customers) {
            if (c.balance > 0 && c.due > 0 && now > c.due) {
                r.overdueCount++;
                r.overdueTotal += c.balance;
            }
            if (c.created >= start && c.created <= now) r.newCustomers++;
        }

        if (r.newDebts > 0) {
            r.percent = (int) Math.min(100, Math.round(r.collected * 100.0 / r.newDebts));
        } else {
            r.percent = r.collected > 0 ? 100 : 0;
        }
        return r;
    }

    /**
     * مجموع الديون والتحصيل لكل شهر من آخر months شهر.
     * النتيجة [i][0] = ديون، [i][1] = تحصيل. i = 0 أقدم شهر، والأخير هالشهر.
     */
    public static long[][] monthly(List<Tx> txs, long now, int months) {
        long[][] out = new long[months][2];
        long[] starts = new long[months + 1];
        for (int i = 0; i < months; i++) starts[i] = monthStart(now, months - 1 - i);
        starts[months] = monthStart(now, -1); // أول الشهر الجاي

        for (Tx t : txs) {
            for (int i = 0; i < months; i++) {
                if (t.date >= starts[i] && t.date < starts[i + 1]) {
                    out[i][t.type == TransactionType.PAYMENT ? 1 : 0] += t.amount;
                    break;
                }
            }
        }
        return out;
    }

    private ReportStats() {}
}
