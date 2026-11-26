package com.example.daftarii.notifications;

import android.content.Context;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/** بيجدول DueReminderWorker مرة باليوم، حوالي الساعة 9 الصبح. */
public final class NotificationScheduler {

    private static final String WORK_NAME = "due_reminders_daily";
    private static final String PREFS = "daftari_notifications_meta";
    private static final String KEY_ENABLED = "reminders_enabled";
    private static final int HOUR = 9;

    private NotificationScheduler() {}

    /** إشعارات الجوال اليومية شغّالة؟ (مفتاح "الإشعارات" بحسابي). افتراضياً آه. */
    public static boolean isEnabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true);
    }

    /** من حسابي: تشغيل أو إيقاف الفحص اليومي. شاشة الإشعارات بالتطبيق بتضل شغّالة بالحالتين. */
    public static void setEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply();
        if (enabled) {
            schedule(context);
        } else {
            cancel(context);
        }
    }

    /** مع تسجيل الخروج أو إيقاف الإشعارات. */
    public static void cancel(Context context) {
        WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
    }

    /** آمن نناديه كل ما التطبيق يفتح: KEEP ما بيعمل جدولة جديدة إذا في وحدة. */
    public static void schedule(Context context) {
        if (!isEnabled(context)) return;
        NotificationHelper.createChannel(context);
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                DueReminderWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(millisUntilNext(HOUR), TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .build();
        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    static long millisUntilNext(int hour) {
        Calendar now = Calendar.getInstance();
        Calendar next = (Calendar) now.clone();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, 0);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);
        if (!next.after(now)) next.add(Calendar.DAY_OF_YEAR, 1);
        return next.getTimeInMillis() - now.getTimeInMillis();
    }
}
