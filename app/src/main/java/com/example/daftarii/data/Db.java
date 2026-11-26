package com.example.daftarii.data;

import androidx.annotation.Nullable;

import com.example.daftarii.Customer;
import com.example.daftarii.DebtItem;
import com.example.daftarii.Transaction;
import com.example.daftarii.logic.TransactionType;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * بنية Firestore المتفق عليها (القسم 13 بالملخص، و firestore.rules):
 *
 *   users/{uid}/customers/{id}     name, phone, dueDate, balance (أغورة long), archived, archivedAt, createdAt
 *   users/{uid}/transactions/{id}  customerId, customerName, type (DEBT|PAYMENT), amount (أغورة long > 0),
 *                                  note, date, archived, archivedAt, editHistory[]
 *
 * هاد المكان الوحيد اللي فيه أسماء المسارات والحقول. الشاشات بتحكي مع
 * CustomerRepository و TransactionRepository بس.
 */
public final class Db {

    public static final String F_NAME = "name";
    public static final String F_PHONE = "phone";
    public static final String F_DUE = "dueDate";
    public static final String F_BALANCE = "balance";
    public static final String F_ARCHIVED = "archived";
    public static final String F_ARCHIVED_AT = "archivedAt";
    public static final String F_CREATED = "createdAt";

    public static final String F_CUSTOMER_ID = "customerId";
    public static final String F_CUSTOMER_NAME = "customerName";
    public static final String F_TYPE = "type";
    public static final String F_AMOUNT = "amount";
    public static final String F_NOTE = "note";
    public static final String F_DATE = "date";
    public static final String F_HISTORY = "editHistory";

    private Db() {}

    @Nullable
    public static String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    public static FirebaseFirestore fs() {
        return FirebaseFirestore.getInstance();
    }

    public static CollectionReference customers(String uid) {
        return fs().collection("users").document(uid).collection("customers");
    }

    public static CollectionReference transactions(String uid) {
        return fs().collection("users").document(uid).collection("transactions");
    }

    public static DocumentReference customer(String uid, String id) {
        return customers(uid).document(id);
    }

    public static DocumentReference transaction(String uid, String id) {
        return transactions(uid).document(id);
    }

    // ---------- من Firestore لموديلات الشاشات (الشاشات لسا بتستخدم الشيكل double) ----------

    public static long lng(DocumentSnapshot d, String field) {
        Long v = d.getLong(field);
        return v != null ? v : 0L;
    }

    public static Customer toCustomer(DocumentSnapshot d) {
        Customer c = new Customer(d.getId(), d.getString(F_NAME), d.getString(F_PHONE),
                lng(d, F_BALANCE) / 100.0, lng(d, F_DUE));
        c.setCreatedAt(lng(d, F_CREATED));
        return c;
    }

    public static TransactionType type(DocumentSnapshot d) {
        return "PAYMENT".equals(d.getString(F_TYPE)) ? TransactionType.PAYMENT : TransactionType.DEBT;
    }

    /** DebtItem: المبلغ بالشيكل، وسالب للدفعة (متل ما الشاشات متعوّدة). */
    public static DebtItem toDebtItem(DocumentSnapshot d) {
        long a = lng(d, F_AMOUNT);
        double signed = (type(d) == TransactionType.PAYMENT ? -a : a) / 100.0;
        DebtItem item = new DebtItem(d.getId(), d.getString(F_NOTE), signed, lng(d, F_DATE));
        item.setCustomerId(d.getString(F_CUSTOMER_ID));
        return item;
    }

    /** Transaction: لشاشة الأرشيف. */
    public static Transaction toTransaction(DocumentSnapshot d) {
        Transaction t = new Transaction(d.getId(), d.getString(F_CUSTOMER_ID), d.getString(F_CUSTOMER_NAME),
                d.getString(F_NOTE), lng(d, F_AMOUNT) / 100.0, d.getString(F_TYPE), lng(d, F_DATE));
        t.setArchived(Boolean.TRUE.equals(d.getBoolean(F_ARCHIVED)));
        t.setDeletedAt(lng(d, F_ARCHIVED_AT));
        return t;
    }
}
