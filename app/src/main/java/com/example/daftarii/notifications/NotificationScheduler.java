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
    private static final int HOUR = 9;

    private NotificationScheduler() {}

    /** آمن نناديه كل ما التطبيق يفتح: KEEP ما بيعمل جدولة جديدة إذا في وحدة. */
    public static void schedule(Context context) {
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
