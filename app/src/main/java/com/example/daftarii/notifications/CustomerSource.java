package com.example.daftarii.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.daftarii.logic.DueAlerts;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * المكان الوحيد بالإشعارات اللي بيعرف كيف الزباين محفوظين بـ Firestore.
 *
 * هلأ (بنية مرام الحالية): customers فيها userId، و totalDebt بالشيكل (double)، و dueDate، و archived.
 * إذا انتقلنا لـ users/{uid}/customers والرصيد بالأغورة، بس هاد الملف بيتغيّر.
 */
public final class CustomerSource {

    private CustomerSource() {}

    public static Task<QuerySnapshot> query(@NonNull String uid) {
        return FirebaseFirestore.getInstance()
                .collection("customers")
                .whereEqualTo("userId", uid)
                .get();
    }

    @NonNull
    public static List<DueAlerts.CustomerInfo> toCustomers(QuerySnapshot snapshot) {
        List<DueAlerts.CustomerInfo> out = new ArrayList<>();
        if (snapshot == null) return out;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            if (Boolean.TRUE.equals(doc.getBoolean("archived"))) continue;
            Double debt = doc.getDouble("totalDebt");
            Long due = doc.getLong("dueDate");
            out.add(new DueAlerts.CustomerInfo(
                    doc.getId(),
                    doc.getString("name"),
                    doc.getString("phone"),
                    debt == null ? 0 : Math.round(debt * 100),
                    due == null ? 0 : due));
        }
        return out;
    }

    /** للـ Worker (مش على الـ main thread). */
    @WorkerThread
    @NonNull
    public static List<DueAlerts.CustomerInfo> loadBlocking(@NonNull String uid) throws Exception {
        return toCustomers(Tasks.await(query(uid), 30, TimeUnit.SECONDS));
    }
}
