package com.example.daftarii;

import com.example.daftarii.data.TransactionRepository;
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

    /**
     * الحركات المؤرشفة لهالحساب بس (users/{uid}/transactions، archived = true).
     * (القديم كان collectionGroup("debts") اللي بيجيب حركات كل المستخدمين.)
     */
    private void listenToArchivedTransactions() {
        listenerRegistration = TransactionRepository.listenArchived(list -> {
            if (!isAdded()) return;
            updateList(list);
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
        // بترجع تنحسب بالرصيد (increment بنفس الـ batch)
        TransactionRepository.restore(transactionId, ok -> {
            if (isAdded() && !ok) {
                Toast.makeText(requireContext(), getString(R.string.archive_restore_failed), Toast.LENGTH_SHORT).show();
            }
        });
        removeTransactionLocally(transactionId);
        Toast.makeText(requireContext(), getString(R.string.archive_tx_restored), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeleteClick(Transaction transaction) {
        if (!isAdded() || transaction.getId() == null || transaction.getCustomerId() == null) return;

        String transactionId = transaction.getId();
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.archive_delete_title)
                .setMessage(R.string.archive_delete_tx_message)
                .setPositiveButton(R.string.action_delete, (dialog, which) -> {
                    TransactionRepository.deleteForever(transactionId, ok -> {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), getString(ok ? R.string.archive_tx_deleted
                                    : R.string.archive_delete_failed), Toast.LENGTH_SHORT).show();
                        }
                    });
                    removeTransactionLocally(transactionId);
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