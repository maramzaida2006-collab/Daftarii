package com.example.daftarii.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.daftarii.logic.DueAlerts;

import java.util.List;

/**
 * بيشتغل مرة باليوم بالخلفية (حتى لو التطبيق مسكّر): بيفحص مواعيد السداد،
 * وبيبعت إشعار على الجوال لكل تذكير جديد. بدون سيرفر.
 */
public class DueReminderWorker extends Worker {

    public DueReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            List<DueAlerts.Item> added = NotificationSync.runBlocking(getApplicationContext());
            NotificationHelper.post(getApplicationContext(), added);
            return Result.success();
        } catch (Exception noInternetOrTimeout) {
            // WorkManager بيرجع يجرّب لحاله بعدين
            return Result.retry();
        }
    }
}
