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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArchivedTransactionsFragment extends Fragment
        implements ArchivedTransactionsAdapter.OnTransactionActionListener {

    private RecyclerView rvArchive;
    private ArchivedTransactionsAdapter adapter;
    private FirebaseFirestore db;
    private ListenerRegistration listenerRegistration;
    private List<Transaction> currentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_archived_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        rvArchive = view.findViewById(R.id.rvArchive);
        rvArchive.setLayoutManager(new LinearLayoutManager(requireContext()));

        adapter = new ArchivedTransactionsAdapter(this);
        rvArchive.setAdapter(adapter);

        getParentFragmentManager().setFragmentResultListener(
                "customer_restored",
                getViewLifecycleOwner(),
                (requestKey, bundle) -> {
                    String restoredCustomerId = bundle.getString("customerId");
                    if (restoredCustomerId != null && !restoredCustomerId.isEmpty()) {
                        removeTransactionsByCustomerId(restoredCustomerId);
                    }
                }
        );

        listenToArchivedTransactions();
    }

    private void listenToArchivedTransactions() {
        listenerRegistration = db.collectionGroup("debts")
                .whereEqualTo("archived", true)
                .orderBy("deletedAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (!isAdded()) return;
                    if (error != null) {
                        // في حال لم يتم إنشاء Index لـ orderBy مع whereEqualTo بعد، نجلب بدون orderBy مؤقتاً
                        loadWithoutOrderFallback();
                        return;
                    }
                    if (snapshots != null) {
                        updateList(parseTransactions(snapshots.getDocuments()));
                    }
                });
    }

    private void loadWithoutOrderFallback() {
        db.collectionGroup("debts")
                .whereEqualTo("archived", true)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!isAdded() || snapshots == null) return;
                    updateList(parseTransactions(snapshots.getDocuments()));
                });
    }

    private void updateList(List<Transaction> list) {
        currentList = new ArrayList<>(list);
        adapter.setTransactions(currentList);
        notifyActivityCount(currentList.size());
    }

    private void removeTransactionLocally(String transactionId) {
        List<Transaction> updated = new ArrayList<>();
        for (Transaction tx : currentList) {
            if (tx.getId() != null && !tx.getId().equals(transactionId)) {
                updated.add(tx);
            }
        }
        updateList(updated);
    }

    private void removeTransactionsByCustomerId(String customerId) {
        List<Transaction> updated = new ArrayList<>();
        for (Transaction tx : currentList) {
            if (tx.getCustomerId() == null || !tx.getCustomerId().equals(customerId)) {
                updated.add(tx);
            }
        }
        updateList(updated);
    }

    private List<Transaction> parseTransactions(List<DocumentSnapshot> docs) {
        List<Transaction> list = new ArrayList<>();
        for (DocumentSnapshot doc : docs) {
            Transaction tx = doc.toObject(Transaction.class);
            if (tx != null) {
                tx.setId(doc.getId());
                // استخراج customerId من المسار: customers/{customerId}/debts/{debtId}
                if (tx.getCustomerId() == null || tx.getCustomerId().isEmpty()) {
                    DocumentReference parentCustomer = doc.getReference().getParent().getParent();
                    if (parentCustomer != null) {
                        tx.setCustomerId(parentCustomer.getId());
                    }
                }
                list.add(tx);
            }
        }
        return list;
    }

    private void notifyActivityCount(int count) {
        if (getActivity() instanceof ArchiveActivity) {
            ((ArchiveActivity) getActivity()).updateTransactionsCount(count);
        }
    }

    @Override
    public void onRestoreClick(Transaction transaction) {
        if (transaction.getId() == null || transaction.getCustomerId() == null) {
            Toast.makeText(requireContext(), getString(R.string.archive_tx_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String transactionId = transaction.getId();
        DocumentReference customerRef = db.collection("customers").document(transaction.getCustomerId());
        DocumentReference debtRef = customerRef.collection("debts").document(transactionId);

        db.runTransaction(firestoreTx -> {
            DocumentSnapshot customerSnap = firestoreTx.get(customerRef);
            double currentDebt = 0.0;
            if (customerSnap.exists() && customerSnap.getDouble("totalDebt") != null) {
                currentDebt = customerSnap.getDouble("totalDebt");
            }

            boolean isPayment = "PAYMENT".equalsIgnoreCase(transaction.getType());
            double delta = isPayment ? -Math.abs(transaction.getAmount()) : Math.abs(transaction.getAmount());
            double updatedDebt = currentDebt + delta;

            Map<String, Object> txUpdates = new HashMap<>();
            txUpdates.put("archived", false);
            txUpdates.put("deletedAt", 0L);

            firestoreTx.update(debtRef, txUpdates);
            if (customerSnap.exists()) {
                firestoreTx.update(customerRef, "totalDebt", updatedDebt);
            }
            return null;
        }).addOnSuccessListener(unused -> {
            if (isAdded()) {
                removeTransactionLocally(transactionId);
                Toast.makeText(requireContext(), getString(R.string.archive_tx_restored), Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                Toast.makeText(requireContext(), getString(R.string.archive_restore_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDeleteClick(Transaction transaction) {
        if (!isAdded() || transaction.getId() == null || transaction.getCustomerId() == null) return;

        String transactionId = transaction.getId();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.archive_delete_title)
                .setMessage(R.string.archive_delete_tx_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    db.collection("customers")
                            .document(transaction.getCustomerId())
                            .collection("debts")
                            .document(transactionId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                if (isAdded()) {
                                    removeTransactionLocally(transactionId);
                                    Toast.makeText(requireContext(), getString(R.string.archive_tx_deleted), Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (isAdded()) {
                                    Toast.makeText(requireContext(), getString(R.string.archive_delete_failed), Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
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