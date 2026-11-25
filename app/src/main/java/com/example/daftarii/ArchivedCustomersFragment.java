package com.example.daftarii;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchivedCustomersFragment extends Fragment
        implements ArchivedCustomersAdapter.OnCustomerActionListener {

    private RecyclerView rvArchivedCustomers;
    private ArchivedCustomersAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration listenerRegistration;
    private List<Customer> currentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archived_customers, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        rvArchivedCustomers = view.findViewById(R.id.rvArchivedCustomers);
        rvArchivedCustomers.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ArchivedCustomersAdapter(this);
        rvArchivedCustomers.setAdapter(adapter);

        listenToArchivedCustomers();
    }

    private String getCurrentUserId() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    private void listenToArchivedCustomers() {
        String uid = getCurrentUserId();
        if (uid == null) return;

        listenerRegistration = db.collection("customers")
                .whereEqualTo("userId", uid)
                .whereEqualTo("archived", true)
                .orderBy("deletedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        loadWithoutOrderFallback(uid);
                        return;
                    }
                    if (snapshots != null) {
                        updateList(parseCustomers(snapshots.getDocuments()));
                    }
                });
    }

    private void loadWithoutOrderFallback(String uid) {
        db.collection("customers")
                .whereEqualTo("userId", uid)
                .whereEqualTo("archived", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded() || snapshots == null) return;
                    updateList(parseCustomers(snapshots.getDocuments()));
                });
    }

    private void updateList(List<Customer> list) {
        currentList = new ArrayList<>(list);
        adapter.setCustomers(currentList);
        notifyActivityCount(currentList.size());
    }

    private List<Customer> parseCustomers(List<DocumentSnapshot> docs) {
        List<Customer> list = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Customer customer = doc.toObject(Customer.class);
            if (customer != null) {
                customer.setId(doc.getId());
                list.add(customer);
            }
        }
        return list;
    }

    private void notifyActivityCount(int count) {
        if (getActivity() instanceof ArchiveActivity) {
            ((ArchiveActivity) getActivity()).updateCustomersCount(count);
        }
    }

    @Override
    public void onRestoreClick(Customer customer) {
        if (customer.getId() == null || customer.getId().isEmpty()) {
            Toast.makeText(requireContext(), "تعذر تحديد بيانات الزبون", Toast.LENGTH_SHORT).show();
            return;
        }

        String customerId = customer.getId();
        db.collection("customers")
                .document(customerId)
                .collection("debts")
                .whereEqualTo("archived", true)
                .get()
                .addOnSuccessListener(debtSnapshots -> {
                    WriteBatch batch = db.batch();

                    Map<String, Object> customerUpdates = new HashMap<>();
                    customerUpdates.put("archived", false);
                    customerUpdates.put("deletedAt", 0L);
                    batch.update(db.collection("customers").document(customerId), customerUpdates);

                    for (QueryDocumentSnapshot debtDoc : debtSnapshots) {
                        Map<String, Object> debtUpdates = new HashMap<>();
                        debtUpdates.put("archived", false);
                        debtUpdates.put("deletedAt", 0L);
                        batch.update(debtDoc.getReference(), debtUpdates);
                    }

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;
                                removeCustomerLocally(customerId);
                                Toast.makeText(
                                        requireContext(),
                                        "تم استرجاع الزبون وحركاته إلى القائمة الرئيسية",
                                        Toast.LENGTH_SHORT
                                ).show();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Toast.makeText(
                                        requireContext(),
                                        "فشل استرجاع الزبون: " + e.getMessage(),
                                        Toast.LENGTH_SHORT
                                ).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(
                            requireContext(),
                            "تعذر الوصول لحركات الزبون: " + e.getMessage(),
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void removeCustomerLocally(String customerId) {
        List<Customer> updated = new ArrayList<>();
        for (Customer c : currentList) {
            if (c.getId() != null && !c.getId().equals(customerId)) {
                updated.add(c);
            }
        }
        updateList(updated);
    }

    @Override
    public void onDeleteClick(Customer customer) {
        if (!isAdded() || customer.getId() == null || customer.getId().isEmpty()) return;

        new AlertDialog.Builder(requireContext())
                .setTitle("حذف نهائي")
                .setMessage("هل أنتِ متأكدة من حذف الزبون «" +
                        (customer.getName() != null ? customer.getName() : "") +
                        "» نهائياً مع حركاته؟ لا يمكن التراجع عن هذا الإجراء.")
                .setPositiveButton("حذف", (dialog, which) -> deleteCustomerWithDebts(customer.getId()))
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void deleteCustomerWithDebts(String customerId) {
        db.collection("customers")
                .document(customerId)
                .collection("debts")
                .get()
                .addOnSuccessListener(debtSnapshots -> {
                    WriteBatch batch = db.batch();
                    for (QueryDocumentSnapshot doc : debtSnapshots) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(db.collection("customers").document(customerId));

                    batch.commit()
                            .addOnSuccessListener(unused -> {
                                if (!isAdded()) return;
                                removeCustomerLocally(customerId);
                                Toast.makeText(requireContext(), "تم حذف الزبون نهائياً", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (!isAdded()) return;
                                Toast.makeText(requireContext(), "فشل الحذف: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(), "تعذر الوصول لحركات الزبون: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}