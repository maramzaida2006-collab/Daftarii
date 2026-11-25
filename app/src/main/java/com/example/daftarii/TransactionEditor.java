package com.example.daftarii;

import androidx.annotation.NonNull;

import com.example.daftarii.logic.TransactionEdit;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * قراءة وحفظ تعديل حركة بـ Firestore (البنية الحالية: customers/{id}/debts/{id}).
 * - الرصيد بيتعدّل بـ FieldValue.increment (مش بقيمة محسوبة على الجوال)،
 *   فلو صار تعديلين ورا بعض أو من جهازين، ما بيضيع إشي.
 * - كل تعديل بينضاف لـ editHistory بنفس الـ batch.
 * إذا انتقلنا لـ users/{uid}/transactions، بس هاد الملف بيتغيّر.
 */
public final class TransactionEditor {

    public interface Callback<T> {
        void onResult(T value);
    }

    private TransactionEditor() {}

    private static DocumentReference debtRef(String customerId, String debtId) {
        return FirebaseFirestore.getInstance().collection("customers").document(customerId)
                .collection("debts").document(debtId);
    }

    /** سجل التعديلات المحفوظ على الحركة. فاضي إذا ما في، أو إذا فشل التحميل. */
    public static void loadHistory(@NonNull String customerId, @NonNull String debtId,
                                   @NonNull Callback<List<TransactionEdit.Entry>> callback) {
        debtRef(customerId, debtId).get().addOnCompleteListener(task -> {
            List<TransactionEdit.Entry> out = new ArrayList<>();
            DocumentSnapshot doc = task.isSuccessful() ? task.getResult() : null;
            Object raw = doc != null ? doc.get("editHistory") : null;
            if (raw instanceof List) {
                for (Object o : (List<?>) raw) {
                    if (!(o instanceof Map)) continue;
                    Map<?, ?> m = (Map<?, ?>) o;
                    out.add(new TransactionEdit.Entry(
                            asLong(m.get("at")), asLong(m.get("oldAmount")), asLong(m.get("newAmount")),
                            asString(m.get("oldNote")), asString(m.get("newNote"))));
                }
            }
            callback.onResult(out);
        });
    }

    /**
     * بيحفظ التعديل: الوصف والمبلغ الجديد (بالشيكل، سالب للدفعة متل باقي الكود)،
     * وزيادة/نقصان رصيد الزبون بالفرق، وسطر بالسجل.
     */
    public static void save(@NonNull String customerId, @NonNull String debtId,
                            @NonNull String newNote, double newSignedAmount, double balanceDiff,
                            @NonNull TransactionEdit.Entry entry, @NonNull Callback<Boolean> callback) {
        Map<String, Object> historyItem = new HashMap<>();
        historyItem.put("at", entry.at);
        historyItem.put("oldAmount", entry.oldAmount);
        historyItem.put("newAmount", entry.newAmount);
        historyItem.put("oldNote", entry.oldNote);
        historyItem.put("newNote", entry.newNote);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();
        batch.update(debtRef(customerId, debtId),
                "itemName", newNote,
                "amount", newSignedAmount,
                "editHistory", FieldValue.arrayUnion(historyItem));
        if (balanceDiff != 0) {
            batch.update(db.collection("customers").document(customerId),
                    "totalDebt", FieldValue.increment(balanceDiff));
        }
        batch.commit().addOnCompleteListener(t -> callback.onResult(t.isSuccessful()));
    }

    private static long asLong(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : 0L;
    }

    private static String asString(Object o) {
        return o instanceof String ? (String) o : "";
    }
}
