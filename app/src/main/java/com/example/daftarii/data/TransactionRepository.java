package com.example.daftarii.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.daftarii.DebtItem;
import com.example.daftarii.Transaction;
import com.example.daftarii.logic.BalanceCalculator;
import com.example.daftarii.logic.TransactionType;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * الحركات: users/{uid}/transactions. كل حركة بتغيّر رصيد الزبون بنفس الـ batch
 * بـ FieldValue.increment (حسب BalanceCalculator)، فالرصيد ما بيغلط حتى لو صار
 * تعديلين ورا بعض أو من جهازين.
 */
public final class TransactionRepository {

    private TransactionRepository() {}

    /** حركات زبون (المش مؤرشفة)، الأحدث فوق. */
    @Nullable
    public static ListenerRegistration listenForCustomer(@NonNull String customerId,
                                                         @NonNull CustomerRepository.Listener<List<DebtItem>> listener) {
        String uid = Db.uid();
        if (uid == null) return null;
        return Db.transactions(uid)
                .whereEqualTo(Db.F_CUSTOMER_ID, customerId)
                .whereEqualTo(Db.F_ARCHIVED, false)
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) return;
                    List<DebtItem> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) out.add(Db.toDebtItem(d));
                    Collections.sort(out, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                    listener.onChanged(out);
                });
    }

    /** مرة وحدة (للـ PDF والتقارير). */
    public static void loadForCustomer(@NonNull String customerId,
                                       @NonNull CustomerRepository.Listener<List<DebtItem>> listener) {
        String uid = Db.uid();
        if (uid == null) {
            listener.onChanged(new ArrayList<>());
            return;
        }
        Db.transactions(uid).whereEqualTo(Db.F_CUSTOMER_ID, customerId).whereEqualTo(Db.F_ARCHIVED, false)
                .get().addOnCompleteListener(t -> {
                    List<DebtItem> out = new ArrayList<>();
                    if (t.isSuccessful() && t.getResult() != null) {
                        for (DocumentSnapshot d : t.getResult().getDocuments()) out.add(Db.toDebtItem(d));
                    }
                    Collections.sort(out, (a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                    listener.onChanged(out);
                });
    }

    /** كل الحركات المش مؤرشفة (للتقارير). */
    public static void loadAll(@NonNull CustomerRepository.Listener<List<Transaction>> listener) {
        String uid = Db.uid();
        if (uid == null) {
            listener.onChanged(new ArrayList<>());
            return;
        }
        Db.transactions(uid).whereEqualTo(Db.F_ARCHIVED, false).get().addOnCompleteListener(t -> {
            List<Transaction> out = new ArrayList<>();
            if (t.isSuccessful() && t.getResult() != null) {
                for (DocumentSnapshot d : t.getResult().getDocuments()) out.add(Db.toTransaction(d));
            }
            listener.onChanged(out);
        });
    }

    /** الحركات المؤرشفة (شاشة الأرشيف)، الأحدث أرشفة فوق. */
    @Nullable
    public static ListenerRegistration listenArchived(@NonNull CustomerRepository.Listener<List<Transaction>> listener) {
        String uid = Db.uid();
        if (uid == null) return null;
        return Db.transactions(uid).whereEqualTo(Db.F_ARCHIVED, true).addSnapshotListener((snap, e) -> {
            if (e != null || snap == null) return;
            List<Transaction> out = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) out.add(Db.toTransaction(d));
            Collections.sort(out, (a, b) -> Long.compare(b.getDeletedAt(), a.getDeletedAt()));
            listener.onChanged(out);
        });
    }

    /** حركة جديدة (دين أو دفعة) + تحديث رصيد الزبون. بيرجّع id الحركة. */
    @Nullable
    public static String add(@NonNull String customerId, @Nullable String customerName, @NonNull TransactionType type,
                             long amountAgorot, @Nullable String note, long date,
                             @NonNull CustomerRepository.Result result) {
        String uid = Db.uid();
        if (uid == null || amountAgorot <= 0) return null;
        WriteBatch batch = Db.fs().batch();
        String id = addToBatch(batch, uid, customerId, customerName, type, amountAgorot, note, date);
        batch.commit().addOnCompleteListener(t -> result.onDone(t.isSuccessful()));
        return id;
    }

    static String addToBatch(WriteBatch batch, String uid, String customerId, @Nullable String customerName,
                             TransactionType type, long amountAgorot, @Nullable String note, long date) {
        DocumentReference ref = Db.transactions(uid).document();
        Map<String, Object> tx = new HashMap<>();
        tx.put(Db.F_CUSTOMER_ID, customerId);
        if (customerName != null) tx.put(Db.F_CUSTOMER_NAME, customerName);
        tx.put(Db.F_TYPE, type.name());
        tx.put(Db.F_AMOUNT, amountAgorot);
        tx.put(Db.F_NOTE, note == null ? "" : note.trim());
        tx.put(Db.F_DATE, date > 0 ? date : System.currentTimeMillis());
        tx.put(Db.F_ARCHIVED, false);
        tx.put(Db.F_ARCHIVED_AT, 0L);
        batch.set(ref, tx);
        batch.update(Db.customer(uid, customerId),
                Db.F_BALANCE, FieldValue.increment(BalanceCalculator.effect(type, amountAgorot)));
        return ref.getId();
    }

    /** أرشفة حركة: ما بتنحسب بالرصيد بعدها. */
    public static void archive(@NonNull String txId, @NonNull CustomerRepository.Result result) {
        setArchived(txId, true, result);
    }

    /** استرجاع حركة: بترجع تنحسب بالرصيد. */
    public static void restore(@NonNull String txId, @NonNull CustomerRepository.Result result) {
        setArchived(txId, false, result);
    }

    private static void setArchived(String txId, boolean archive, CustomerRepository.Result result) {
        String uid = Db.uid();
        if (uid == null) return;
        DocumentReference ref = Db.transaction(uid, txId);
        // بنقرأ الحركة أول عشان نعرف أثرها على الرصيد (وما نأرشف مرتين)
        ref.get().addOnCompleteListener(t -> {
            DocumentSnapshot d = t.isSuccessful() ? t.getResult() : null;
            if (d == null || !d.exists()) {
                result.onDone(false);
                return;
            }
            boolean already = Boolean.TRUE.equals(d.getBoolean(Db.F_ARCHIVED));
            if (already == archive) {
                result.onDone(true);
                return;
            }
            TransactionType type = Db.type(d);
            long amount = Db.lng(d, Db.F_AMOUNT);
            long effect = archive ? BalanceCalculator.archiveEffect(type, amount)
                    : BalanceCalculator.restoreEffect(type, amount);
            Map<String, Object> u = new HashMap<>();
            u.put(Db.F_ARCHIVED, archive);
            u.put(Db.F_ARCHIVED_AT, archive ? System.currentTimeMillis() : 0L);
            WriteBatch batch = Db.fs().batch();
            batch.update(ref, u);
            batch.update(Db.customer(uid, d.getString(Db.F_CUSTOMER_ID)),
                    Db.F_BALANCE, FieldValue.increment(effect));
            batch.commit().addOnCompleteListener(c -> result.onDone(c.isSuccessful()));
        });
    }

    /** حذف نهائي (للمؤرشفة بس، فما بتأثر على الرصيد). */
    public static void deleteForever(@NonNull String txId, @NonNull CustomerRepository.Result result) {
        String uid = Db.uid();
        if (uid == null) return;
        Db.transaction(uid, txId).delete().addOnCompleteListener(t -> result.onDone(t.isSuccessful()));
    }

    /** عدد الحركات المؤرشفة (لحسابي). */
    public static void countArchived(@NonNull CustomerRepository.Listener<Integer> listener) {
        String uid = Db.uid();
        if (uid == null) {
            listener.onChanged(0);
            return;
        }
        Db.transactions(uid).whereEqualTo(Db.F_ARCHIVED, true).get().addOnCompleteListener(t ->
                listener.onChanged(t.isSuccessful() && t.getResult() != null ? t.getResult().size() : 0));
    }
}
