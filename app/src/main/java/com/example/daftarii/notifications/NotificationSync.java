package com.example.daftarii.notifications;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.daftarii.auth.AuthRepository;
import com.example.daftarii.logic.DueAlerts;
import com.example.daftarii.logic.GuestTrial;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/**
 * بيفحص الزباين، وبيضيف التذكيرات الجديدة على الإشعارات المحفوظة (بدون تكرار).
 * بيشتغل لما الرئيسية تنفتح (async)، ومرة باليوم من DueReminderWorker (blocking).
 */
public final class NotificationSync {

    public interface Callback {
        /** all: كل الإشعارات بعد الدمج. added: اللي انضافوا هلأ. */
        void onDone(@NonNull List<DueAlerts.Item> all, @NonNull List<DueAlerts.Item> added);
    }

    private NotificationSync() {}

    /** من الشاشات. النتيجة على الـ main thread. إذا Firestore فشل بيرجّع المحفوظ متل ما هو. */
    public static void run(@NonNull Context context, @NonNull Callback callback) {
        Context app = context.getApplicationContext();
        AuthRepository repo = new AuthRepository();
        String uid = repo.currentUid();
        NotificationStore store = new NotificationStore(app);
        if (uid == null) {
            callback.onDone(new ArrayList<>(), new ArrayList<>());
            return;
        }
        CustomerSource.query(uid).addOnCompleteListener(task -> {
            List<DueAlerts.Item> saved = store.load(uid);
            if (!task.isSuccessful()) {
                callback.onDone(saved, new ArrayList<>());
                return;
            }
            List<DueAlerts.Item> after = mergeAndSave(store, repo, uid, saved,
                    CustomerSource.toCustomers(task.getResult()));
            callback.onDone(after, DueAlerts.added(saved, after));
        });
    }

    /** من الـ Worker. بيرجّع اللي انضافوا (عشان نبعتهم إشعار على الجوال). */
    @WorkerThread
    @NonNull
    public static List<DueAlerts.Item> runBlocking(@NonNull Context context) throws Exception {
        Context app = context.getApplicationContext();
        AuthRepository repo = new AuthRepository();
        String uid = repo.currentUid();
        if (uid == null) return new ArrayList<>();
        NotificationStore store = new NotificationStore(app);
        List<DueAlerts.Item> saved = store.load(uid);
        List<DueAlerts.Item> after = mergeAndSave(store, repo, uid, saved, CustomerSource.loadBlocking(uid));
        return DueAlerts.added(saved, after);
    }

    private static List<DueAlerts.Item> mergeAndSave(NotificationStore store, AuthRepository repo, String uid,
                                                      List<DueAlerts.Item> saved,
                                                      List<DueAlerts.CustomerInfo> customers) {
        long now = System.currentTimeMillis();
        List<DueAlerts.Item> fresh = DueAlerts.fromCustomers(customers, now, TimeZone.getDefault());
        if (repo.isGuest()) {
            DueAlerts.Item trial = DueAlerts.trialEnding(uid,
                    GuestTrial.daysLeft(repo.accountCreatedAt(), now), now);
            if (trial != null) fresh.add(trial);
        }
        List<DueAlerts.Item> after = DueAlerts.merge(saved, fresh, now);
        store.save(uid, after);
        return after;
    }

    public static void markAllRead(@NonNull Context context) {
        String uid = new AuthRepository().currentUid();
        if (uid == null) return;
        NotificationStore store = new NotificationStore(context);
        store.save(uid, DueAlerts.markAllRead(store.load(uid)));
    }

    public static void markRead(@NonNull Context context, @NonNull String key) {
        String uid = new AuthRepository().currentUid();
        if (uid == null) return;
        NotificationStore store = new NotificationStore(context);
        store.save(uid, DueAlerts.markRead(store.load(uid), key));
    }

    @NonNull
    public static List<DueAlerts.Item> saved(@NonNull Context context) {
        String uid = new AuthRepository().currentUid();
        return uid == null ? new ArrayList<>() : new NotificationStore(context).load(uid);
    }
}
