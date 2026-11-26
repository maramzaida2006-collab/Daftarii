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
import com.example.daftarii.data.CustomerRepository;
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

    /** الزباين المؤرشفين (users/{uid}/customers، archived = true)، الأحدث أرشفة فوق. */
    private void listenToArchivedCustomers() {
        listenerRegistration = CustomerRepository.listen(true, customers -> {
            if (!isAdded()) return;
            updateList(customers);
        }, null);
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
            Toast.makeText(requireContext(), getString(R.string.archive_customer_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String customerId = customer.getId();
        // الزبون بيرجع للرئيسية مع حركاته ورصيده (الأرشفة ما غيّرت عليهم)
        CustomerRepository.restore(customerId, ok -> {
            if (isAdded() && !ok) {
                Toast.makeText(requireContext(), getString(R.string.archive_restore_failed), Toast.LENGTH_SHORT).show();
            }
        });
        removeCustomerLocally(customerId);
        Toast.makeText(requireContext(), getString(R.string.archive_customer_restored), Toast.LENGTH_SHORT).show();
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
                .setTitle(R.string.archive_delete_title)
                .setMessage(getString(R.string.archive_delete_customer_message,
                        customer.getName() != null ? customer.getName() : ""))
                .setPositiveButton(R.string.action_delete, (dialog, which) -> deleteCustomerWithDebts(customer.getId()))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteCustomerWithDebts(String customerId) {
        CustomerRepository.deleteForever(customerId, ok -> {
            if (!isAdded()) return;
            Toast.makeText(requireContext(), getString(ok ? R.string.archive_customer_deleted
                    : R.string.archive_delete_failed), Toast.LENGTH_SHORT).show();
        });
        removeCustomerLocally(customerId);
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