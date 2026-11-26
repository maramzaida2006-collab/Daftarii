package com.example.daftarii.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.daftarii.Customer;
import com.example.daftarii.logic.TransactionType;
import com.example.daftarii.logic.Validators;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** الزباين: users/{uid}/customers. كل كتابة بـ batch، والرصيد ما بينكتب إلا بـ increment. */
public final class CustomerRepository {

    public interface Listener<T> {
        void onChanged(T value);
    }

    public interface Result {
        /** ok=false: فشل (مثلاً القواعد رفضت). Firestore بيحفظ محلياً بدون نت وبيكمّل لحاله. */
        void onDone(boolean ok);
    }

    private CustomerRepository() {}

    /** الزباين المش مؤرشفين، لحظة بلحظة. null إذا ما في مستخدم. لازم remove() بـ onStop/onDestroy. */
    @Nullable
    public static ListenerRegistration listen(boolean archived, @NonNull Listener<List<Customer>> listener,
                                              @Nullable Listener<Exception> onError) {
        String uid = Db.uid();
        if (uid == null) return null;
        return Db.customers(uid).whereEqualTo(Db.F_ARCHIVED, archived)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        if (onError != null) onError.onChanged(e);
                        return;
                    }
                    List<Customer> out = new ArrayList<>();
                    if (snap != null) for (DocumentSnapshot d : snap.getDocuments()) out.add(Db.toCustomer(d));
                    listener.onChanged(out);
                });
    }

    /** زبون واحد لحظة بلحظة (كشف الزبون). */
    @Nullable
    public static ListenerRegistration listenOne(@NonNull String id, @NonNull Listener<Customer> listener) {
        String uid = Db.uid();
        if (uid == null) return null;
        return Db.customer(uid, id).addSnapshotListener((d, e) -> {
            if (e == null && d != null && d.exists()) listener.onChanged(Db.toCustomer(d));
        });
    }

    /** رقم الجوال (بعد التنظيف) مستخدم لزبون تاني؟ */
    public static void phoneExists(@Nullable String phone, @NonNull Listener<Boolean> listener) {
        String uid = Db.uid();
        String p = Validators.normalizePhone(phone);
        if (uid == null || p.isEmpty()) {
            listener.onChanged(false);
            return;
        }
        Db.customers(uid).whereEqualTo(Db.F_PHONE, p).limit(1).get()
                .addOnCompleteListener(t -> listener.onChanged(
                        t.isSuccessful() && t.getResult() != null && !t.getResult().isEmpty()));
    }

    /**
     * زبون جديد، ومعه أول دين إذا firstDebtAgorot > 0 (بنفس الـ batch).
     * بيرجّع id الزبون (أو null إذا ما في مستخدم).
     */
    @Nullable
    public static String add(@NonNull String name, @Nullable String phone, long dueDate,
                             long firstDebtAgorot, @Nullable String firstNote, @NonNull Result result) {
        String uid = Db.uid();
        if (uid == null) return null;
        DocumentReference ref = Db.customers(uid).document();
        long now = System.currentTimeMillis();
        Map<String, Object> c = new HashMap<>();
        c.put(Db.F_NAME, name.trim());
        String p = Validators.normalizePhone(phone);
        c.put(Db.F_PHONE, p.isEmpty() ? null : p);
        c.put(Db.F_DUE, dueDate > 0 ? dueDate : 0L);
        c.put(Db.F_BALANCE, 0L);
        c.put(Db.F_ARCHIVED, false);
        c.put(Db.F_CREATED, now);

        WriteBatch batch = Db.fs().batch();
        batch.set(ref, c);
        if (firstDebtAgorot > 0) {
            TransactionRepository.addToBatch(batch, uid, ref.getId(), name.trim(), TransactionType.DEBT,
                    firstDebtAgorot, firstNote, now);
        }
        batch.commit().addOnCompleteListener(t -> result.onDone(t.isSuccessful()));
        return ref.getId();
    }

    public static void updateDueDate(@NonNull String id, long dueDate, @NonNull Result result) {
        String uid = Db.uid();
        if (uid == null) return;
        Db.customer(uid, id).update(Db.F_DUE, dueDate > 0 ? dueDate : 0L)
                .addOnCompleteListener(t -> result.onDone(t.isSuccessful()));
    }

    /** أرشفة زبون: بيختفي من الرئيسية. حركاته ورصيده بيضلوا متل ما هم. */
    public static void archive(@NonNull String id, @NonNull Result result) {
        setArchived(id, true, result);
    }

    public static void restore(@NonNull String id, @NonNull Result result) {
        setArchived(id, false, result);
    }

    private static void setArchived(String id, boolean archived, Result result) {
        String uid = Db.uid();
        if (uid == null) return;
        Map<String, Object> u = new HashMap<>();
        u.put(Db.F_ARCHIVED, archived);
        u.put(Db.F_ARCHIVED_AT, archived ? System.currentTimeMillis() : 0L);
        Db.customer(uid, id).update(u).addOnCompleteListener(t -> result.onDone(t.isSuccessful()));
    }

    /** حذف نهائي (من الأرشيف بس): الزبون وكل حركاته. */
    public static void deleteForever(@NonNull String id, @NonNull Result result) {
        String uid = Db.uid();
        if (uid == null) return;
        Db.transactions(uid).whereEqualTo(Db.F_CUSTOMER_ID, id).get().addOnCompleteListener(t -> {
            if (!t.isSuccessful() || t.getResult() == null) {
                result.onDone(false);
                return;
            }
            WriteBatch batch = Db.fs().batch();
            for (DocumentSnapshot d : t.getResult().getDocuments()) batch.delete(d.getReference());
            batch.delete(Db.customer(uid, id));
            batch.commit().addOnCompleteListener(c -> result.onDone(c.isSuccessful()));
        });
    }

    /** عدد الزباين المؤرشفين (لحسابي). */
    public static void countArchived(@NonNull Listener<Integer> listener) {
        String uid = Db.uid();
        if (uid == null) {
            listener.onChanged(0);
            return;
        }
        Db.customers(uid).whereEqualTo(Db.F_ARCHIVED, true).get().addOnCompleteListener(t ->
                listener.onChanged(t.isSuccessful() && t.getResult() != null ? t.getResult().size() : 0));
    }
}
