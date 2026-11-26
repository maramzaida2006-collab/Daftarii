package com.example.daftarii.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.daftarii.data.Db;
import com.example.daftarii.logic.DueAlerts;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * المكان الوحيد بالإشعارات اللي بيعرف كيف الزباين محفوظين بـ Firestore.
 *
 * البنية: users/{uid}/customers (المسارات والحقول بـ data/Db).
 */
public final class CustomerSource {

    private CustomerSource() {}

    public static Task<QuerySnapshot> query(@NonNull String uid) {
        return Db.customers(uid).whereEqualTo(Db.F_ARCHIVED, false).get();
    }

    @NonNull
    public static List<DueAlerts.CustomerInfo> toCustomers(QuerySnapshot snapshot) {
        List<DueAlerts.CustomerInfo> out = new ArrayList<>();
        if (snapshot == null) return out;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            out.add(new DueAlerts.CustomerInfo(
                    doc.getId(),
                    doc.getString(Db.F_NAME),
                    doc.getString(Db.F_PHONE),
                    Db.lng(doc, Db.F_BALANCE),
                    Db.lng(doc, Db.F_DUE)));
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
