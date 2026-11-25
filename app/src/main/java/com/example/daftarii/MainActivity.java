package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.daftarii.auth.PinStore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // Header
    private TextView tvStoreInitial, tvGreeting, tvStoreName;
    private FrameLayout btnNotifications;
    private ImageView ivNotificationBell;
    private View viewUnreadDot;

    // Summary Card
    private TextView tvTotalDueAmount, tvCollectedAmount, tvDebtorsCount, tvOverdueCount;

    // Quick Actions
    private LinearLayout btnNewDebt, btnNewPayment, btnAddCustomer, btnSearchCustomers;

    // Follow-up & Customers Section
    private TextView btnViewAllFollowUp, tvSortHighestDebt;
    private RecyclerView rvFollowUpCustomers, rvCustomersList;

    // Bottom Navigation
    private LinearLayout navTabHome;
    private FrameLayout navTabReports, navTabProfile;

    private CustomersAdapter customersAdapter;
    private CustomersAdapter followUpAdapter;

    private final List<Customer> customerList = new ArrayList<>();
    private final List<Customer> displayedCustomersList = new ArrayList<>();
    private final List<Customer> followUpList = new ArrayList<>();

    private boolean sortByHighestDebt = true;
    private String activeSearchQuery = "";

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentUserId;
    private ListenerRegistration customersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();

        db = FirebaseFirestore.getInstance();
        db.setFirestoreSettings(settings);
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerViews();
        setupListeners();
    }

    private void initViews() {
        tvStoreInitial = findViewById(R.id.tvStoreInitial);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStoreName = findViewById(R.id.tvStoreName);
        btnNotifications = findViewById(R.id.btnNotifications);
        ivNotificationBell = findViewById(R.id.ivNotificationBell);
        viewUnreadDot = findViewById(R.id.viewUnreadDot);

        tvTotalDueAmount = findViewById(R.id.tvTotalDueAmount);
        tvCollectedAmount = findViewById(R.id.tvCollectedAmount);
        tvDebtorsCount = findViewById(R.id.tvDebtorsCount);
        tvOverdueCount = findViewById(R.id.tvOverdueCount);

        btnNewDebt = findViewById(R.id.btnNewDebt);
        btnNewPayment = findViewById(R.id.btnNewPayment);
        btnAddCustomer = findViewById(R.id.btnAddCustomer);
        btnSearchCustomers = findViewById(R.id.btnSearchCustomers);

        btnViewAllFollowUp = findViewById(R.id.btnViewAllFollowUp);
        tvSortHighestDebt = findViewById(R.id.tvSortHighestDebt);
        rvFollowUpCustomers = findViewById(R.id.rvFollowUpCustomers);
        rvCustomersList = findViewById(R.id.rvCustomersList);

        navTabHome = findViewById(R.id.navTabHome);
        navTabReports = findViewById(R.id.navTabReports);
        navTabProfile = findViewById(R.id.navTabProfile);
    }

    private void setupRecyclerViews() {
        customersAdapter = new CustomersAdapter(displayedCustomersList, this::openCustomerDetails);
        rvCustomersList.setLayoutManager(new LinearLayoutManager(this));
        rvCustomersList.setAdapter(customersAdapter);

        followUpAdapter = new CustomersAdapter(followUpList, this::openCustomerDetails);
        rvFollowUpCustomers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvFollowUpCustomers.setAdapter(followUpAdapter);
    }

    private void openCustomerDetails(Customer customer) {
        Intent intent = new Intent(MainActivity.this, CustomerDetailActivity.class);
        intent.putExtra("CUSTOMER_ID", customer.getId());
        intent.putExtra("CUSTOMER_NAME", customer.getName());
        intent.putExtra("CUSTOMER_PHONE", customer.getPhone());
        intent.putExtra("CUSTOMER_TOTAL_DEBT", customer.getTotalDebt());
        startActivity(intent);
    }

    private void setupListeners() {
        btnNotifications.setOnClickListener(v -> {
            if (viewUnreadDot != null) {
                viewUnreadDot.setVisibility(View.GONE);
            }
            showOverdueNotificationsDialog();
        });

        // فتح البوتوم شيت الموحّد للحركة (دين جديد / دفعة)
        btnNewDebt.setOnClickListener(v -> openTransactionSheet(true));
        btnNewPayment.setOnClickListener(v -> openTransactionSheet(false));

        // فتح البوتوم شيت الخاص بإضافة زبون جديد
        btnAddCustomer.setOnClickListener(v -> openNewCustomerSheet());

        // البحث في الزبائن
        btnSearchCustomers.setOnClickListener(v -> showSearchDialog());

        // عرض جميع الزبائن بحاجة لمتابعة
        btnViewAllFollowUp.setOnClickListener(v -> showAllFollowUpDialog());

        // التبديل بين الترتيب حسب الأعلى ديناً والأبجدي
        tvSortHighestDebt.setOnClickListener(v -> {
            sortByHighestDebt = !sortByHighestDebt;
            tvSortHighestDebt.setAlpha(sortByHighestDebt ? 1.0f : 0.7f);
            refreshListsAndSummary();
        });

        // الشريط السفلي العائم
        navTabHome.setOnClickListener(v -> {
            activeSearchQuery = "";
            refreshListsAndSummary();
        });

        navTabReports.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ReportsActivity.class))
        );

        navTabProfile.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ProfileActivity.class))
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserAuthAndLoad();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (customersListener != null) {
            customersListener.remove();
            customersListener = null;
        }
    }

    private void checkUserAuthAndLoad() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            navigateToLogin();
            return;
        }
        if (!user.isAnonymous() && !user.isEmailVerified()) {
            Intent intent = new Intent(MainActivity.this, VerifyEmailActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        currentUserId = user.getUid();
        bindStoreHeader(user);
        loadCustomers();
    }

    private void bindStoreHeader(FirebaseUser user) {
        String storeTitle = getString(R.string.home_store_name);
        if (user.isAnonymous()) {
            storeTitle = getString(R.string.guest_store_name_fallback, storeTitle);
        } else if (user.getDisplayName() != null && !user.getDisplayName().trim().isEmpty()) {
            storeTitle = user.getDisplayName().trim();
        }
        tvStoreName.setText(storeTitle);

        String trimmed = storeTitle.trim();
        if (!trimmed.isEmpty()) {
            tvStoreInitial.setText(trimmed.substring(0, 1));
        }
    }

    private void navigateToLogin() {
        new PinStore(this).clear();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadCustomers() {
        if (currentUserId == null || mAuth.getCurrentUser() == null) return;

        if (customersListener != null) {
            customersListener.remove();
        }

        customersListener = db.collection("customers")
                .whereEqualTo("userId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        if (mAuth.getCurrentUser() == null) {
                            return;
                        }
                        Toast.makeText(this, "خطأ في التحميل من Firestore: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    customerList.clear();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Boolean archived = doc.getBoolean("archived");
                            if (archived != null && archived) {
                                continue;
                            }
                            Customer customer = doc.toObject(Customer.class);
                            if (customer == null) continue;
                            customer.setId(doc.getId());
                            customerList.add(customer);
                        }
                    }

                    refreshListsAndSummary();
                });
    }

    private void refreshListsAndSummary() {
        long now = System.currentTimeMillis();
        double totalDue = 0.0;
        int debtorsCount = 0;
        int overdueCount = 0;

        followUpList.clear();
        displayedCustomersList.clear();

        String query = activeSearchQuery != null ? activeSearchQuery.toLowerCase().trim() : "";

        for (Customer customer : customerList) {
            double debt = customer.getTotalDebt();
            if (debt > 0) {
                totalDue += debt;
                debtorsCount++;
            }

            long dueDate = customer.getDueDate();
            boolean isOverdue = debt > 0 && dueDate > 0 && dueDate < now;
            if (isOverdue) {
                overdueCount++;
                followUpList.add(customer);
            }

            boolean matchesQuery = query.isEmpty()
                    || (customer.getName() != null && customer.getName().toLowerCase().contains(query))
                    || (customer.getPhone() != null && customer.getPhone().toLowerCase().contains(query));

            if (matchesQuery) {
                displayedCustomersList.add(customer);
            }
        }

        if (sortByHighestDebt) {
            Collections.sort(displayedCustomersList, (a, b) -> Double.compare(b.getTotalDebt(), a.getTotalDebt()));
        } else {
            Collections.sort(displayedCustomersList, (a, b) -> {
                String nameA = a.getName() != null ? a.getName() : "";
                String nameB = b.getName() != null ? b.getName() : "";
                return nameA.compareToIgnoreCase(nameB);
            });
        }

        double roundedDue = Math.round(totalDue * 100.0) / 100.0;
        tvTotalDueAmount.setText(String.format(Locale.getDefault(), "₪ %.2f", roundedDue));
        tvDebtorsCount.setText(String.valueOf(debtorsCount));
        tvOverdueCount.setText(String.valueOf(overdueCount));

        if (viewUnreadDot != null) {
            viewUnreadDot.setVisibility(overdueCount > 0 ? View.VISIBLE : View.GONE);
        }

        customersAdapter.notifyDataSetChanged();
        followUpAdapter.notifyDataSetChanged();

        calculateMonthlyCollected();
    }

    private void calculateMonthlyCollected() {
        if (currentUserId == null || customerList.isEmpty()) {
            tvCollectedAmount.setText(String.format(Locale.getDefault(), "₪ %.2f", 0.0));
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStartMillis = cal.getTimeInMillis();

        final double[] monthlyCollected = {0.0};
        final int[] remaining = {customerList.size()};

        for (Customer c : customerList) {
            if (c.getId() == null) {
                if (--remaining[0] == 0) {
                    tvCollectedAmount.setText(String.format(Locale.getDefault(), "₪ %.2f", monthlyCollected[0]));
                }
                continue;
            }

            db.collection("customers").document(c.getId())
                    .collection("debts")
                    .whereGreaterThanOrEqualTo("timestamp", monthStartMillis)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Boolean archived = doc.getBoolean("archived");
                            if (archived != null && archived) continue;
                            Double amt = doc.getDouble("amount");
                            if (amt != null && amt < 0) {
                                monthlyCollected[0] += Math.abs(amt);
                            }
                        }
                        if (--remaining[0] == 0) {
                            double rounded = Math.round(monthlyCollected[0] * 100.0) / 100.0;
                            tvCollectedAmount.setText(String.format(Locale.getDefault(), "₪ %.2f", rounded));
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (--remaining[0] == 0) {
                            double rounded = Math.round(monthlyCollected[0] * 100.0) / 100.0;
                            tvCollectedAmount.setText(String.format(Locale.getDefault(), "₪ %.2f", rounded));
                        }
                    });
        }
    }

    private void openNewCustomerSheet() {
        if (currentUserId == null) {
            Toast.makeText(this, "جاري تهيئة الحساب، يرجى الانتظار قليلاً...", Toast.LENGTH_SHORT).show();
            return;
        }
        NewCustomerBottomSheetFragment sheet = NewCustomerBottomSheetFragment.newInstance(customerList);
        sheet.show(getSupportFragmentManager(), "NewCustomerBottomSheet");
    }

    private void openTransactionSheet(boolean isDebt) {
        if (customerList.isEmpty()) {
            Toast.makeText(this, R.string.add_customer_first, Toast.LENGTH_SHORT).show();
            return;
        }
        AddTransactionBottomSheetFragment sheet = AddTransactionBottomSheetFragment.newInstance(isDebt, customerList);
        sheet.show(getSupportFragmentManager(), "AddTransactionBottomSheet");
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.action_search);

        final EditText input = new EditText(this);
        input.setHint(R.string.search_customer_hint);
        input.setText(activeSearchQuery);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 30, 50, 10);
        container.addView(input);
        builder.setView(container);

        builder.setPositiveButton(R.string.action_search, (dialog, which) -> {
            activeSearchQuery = input.getText().toString().trim();
            refreshListsAndSummary();
        });

        builder.setNeutralButton(R.string.action_clear_search, (dialog, which) -> {
            activeSearchQuery = "";
            refreshListsAndSummary();
        });

        builder.setNegativeButton(R.string.action_cancel, null);
        builder.show();
    }

    private void showOverdueNotificationsDialog() {
        if (followUpList.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.home_notifications_cd)
                    .setMessage(R.string.no_overdue_customers)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        String[] items = new String[followUpList.size()];
        for (int i = 0; i < followUpList.size(); i++) {
            Customer c = followUpList.get(i);
            items[i] = c.getName() + " — " + String.format(Locale.getDefault(), "₪ %.2f", c.getTotalDebt());
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.section_follow_up)
                .setItems(items, (dialog, which) -> openCustomerDetails(followUpList.get(which)))
                .setPositiveButton(R.string.action_cancel, null)
                .show();
    }

    private void showAllFollowUpDialog() {
        List<Customer> candidates = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Customer c : customerList) {
            if (c.getTotalDebt() > 0 && c.getDueDate() > 0 && c.getDueDate() < now + (3L * 24 * 60 * 60 * 1000)) {
                candidates.add(c);
            }
        }
        if (candidates.isEmpty()) {
            candidates.addAll(followUpList);
        }

        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.no_follow_up_customers, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            Customer c = candidates.get(i);
            items[i] = c.getName() + " (" + String.format(Locale.getDefault(), "₪ %.2f", c.getTotalDebt()) + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.section_follow_up)
                .setItems(items, (dialog, which) -> openCustomerDetails(candidates.get(which)))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }
}