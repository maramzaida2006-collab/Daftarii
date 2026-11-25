package com.example.daftarii.notifications;

import android.content.Context;

import com.example.daftarii.R;
import com.example.daftarii.logic.DueAlerts;
import com.example.daftarii.logic.Money;

import java.util.TimeZone;

/** عنوان ونص كل إشعار (نفس النص بالشاشة وبإشعار الجوال). كل النصوص من strings.xml. */
public final class NotificationText {

    private NotificationText() {}

    public static String title(Context c, DueAlerts.Item i) {
        switch (i.type) {
            case OVERDUE:
                return c.getString(R.string.notif_overdue_title, name(c, i));
            case DUE_TODAY:
                return c.getString(R.string.notif_today_title);
            case DUE_TOMORROW:
                return c.getString(R.string.notif_tomorrow_title);
            case TRIAL_ENDING:
            default:
                return c.getString(R.string.notif_trial_title);
        }
    }

    public static String body(Context c, DueAlerts.Item i) {
        String amount = Money.format(i.amountAgorot);
        switch (i.type) {
            case OVERDUE:
                int days = DueAlerts.daysLate(i.dueDateMillis, System.currentTimeMillis(), TimeZone.getDefault());
                return c.getResources().getQuantityString(R.plurals.notif_overdue_body, days, days, amount);
            case DUE_TODAY:
                return c.getString(R.string.notif_today_body, name(c, i), amount);
            case DUE_TOMORROW:
                return c.getString(R.string.notif_tomorrow_body, name(c, i), amount);
            case TRIAL_ENDING:
            default:
                return i.dueDateMillis <= 1
                        ? c.getString(R.string.notif_trial_body_last)
                        : c.getString(R.string.notif_trial_body, (int) i.dueDateMillis);
        }
    }

    static String name(Context c, DueAlerts.Item i) {
        return i.customerName != null && !i.customerName.trim().isEmpty()
                ? i.customerName.trim() : c.getString(R.string.notif_unknown_customer);
    }
}
