package com.example.daftarii;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class ReportsActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private View btnReportsNotifications;
    private TextView tabThisMonth, tabThreeMonths, tabThisYear;

    private TextView tvCollectionSummary, tvCollectionPercent;
    private TextView tvStatNewDebtsAmount, tvStatNewDebtsCount;
    private TextView tvStatCollectedAmount, tvStatCollectedCount;
    private TextView tvStatOverdueCount, tvStatOverdueTotal;
    private TextView tvStatNewCustomersCount, tvStatNewCustomersSub;

    private FrameLayout navReportsTabHome, navReportsTabProfile;
    private LinearLayout navReportsTabActive;

    private String selectedPeriod = "THIS_MONTH"; // THIS_MONTH, THREE_MONTHS, THIS_YEAR

    private double totalDebtsBalance = 0.0;
    private int totalCustomers = 0;
    private int overdueCustomers = 0;
    private double overdueAmount = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        listenToReportsFromFirestore();
    }

    private void initViews() {
        btnReportsNotifications = findViewById(R.id.btnReportsNotifications);

        tabThisMonth = findViewById(R.id.tabThisMonth);
        tabThreeMonths = findViewById(R.id.tabThreeMonths);
        tabThisYear = findViewById(R.id.tabThisYear);

        tvCollectionSummary = findViewById(R.id.tvCollectionSummary);
        tvCollectionPercent = findViewById(R.id.tvCollectionPercent);

        tvStatNewDebtsAmount = findViewById(R.id.tvStatNewDebtsAmount);
        tvStatNewDebtsCount = findViewById(R.id.tvStatNewDebtsCount);

        tvStatCollectedAmount = findViewById(R.id.tvStatCollectedAmount);
        tvStatCollectedCount = findViewById(R.id.tvStatCollectedCount);

        tvStatOverdueCount = findViewById(R.id.tvStatOverdueCount);
        tvStatOverdueTotal = findViewById(R.id.tvStatOverdueTotal);

        tvStatNewCustomersCount = findViewById(R.id.tvStatNewCustomersCount);
        tvStatNewCustomersSub = findViewById(R.id.tvStatNewCustomersSub);

        navReportsTabHome = findViewById(R.id.navReportsTabHome);
        navReportsTabActive = findViewById(R.id.navReportsTabActive);
        navReportsTabProfile = findViewById(R.id.navReportsTabProfile);
    }

    private void setupListeners() {
        if (btnReportsNotifications != null) {
            btnReportsNotifications.setOnClickListener(v ->
                    Toast.makeText(this, "إشعارات التقارير", Toast.LENGTH_SHORT).show());
        }

        if (tabThisMonth != null) {
            tabThisMonth.setOnClickListener(v -> selectPeriod("THIS_MONTH"));
        }
        if (tabThreeMonths != null) {
            tabThreeMonths.setOnClickListener(v -> selectPeriod("THREE_MONTHS"));
        }
        if (tabThisYear != null) {
            tabThisYear.setOnClickListener(v -> selectPeriod("THIS_YEAR"));
        }

        if (navReportsTabHome != null) {
            navReportsTabHome.setOnClickListener(v -> {
                startActivity(new Intent(ReportsActivity.this, MainActivity.class));
                finish();
            });
        }

        if (navReportsTabActive != null) {
            navReportsTabActive.setOnClickListener(v -> {
                // نحن بالفعل في شاشة التقارير
            });
        }

        if (navReportsTabProfile != null) {
            navReportsTabProfile.setOnClickListener(v -> {
                startActivity(new Intent(ReportsActivity.this, ProfileActivity.class));
                finish();
            });
        }
    }

    private void selectPeriod(String period) {
        selectedPeriod = period;
        highlightPeriodTab(period);
        updateStatsUi();
    }

    private void highlightPeriodTab(String selected) {
        if (tabThisMonth == null || tabThreeMonths == null || tabThisYear == null) return;

        tabThisMonth.setTextColor(getColor(selected.equals("THIS_MONTH") ? R.color.text_primary : R.color.text_secondary));
        tabThreeMonths.setTextColor(getColor(selected.equals("THREE_MONTHS") ? R.color.text_primary : R.color.text_secondary));
        tabThisYear.setTextColor(getColor(selected.equals("THIS_YEAR") ? R.color.text_primary : R.color.text_secondary));

        tabThisMonth.setBackgroundTintList(getColorStateList(selected.equals("THIS_MONTH") ? R.color.background : android.R.color.transparent));
        tabThreeMonths.setBackgroundTintList(getColorStateList(selected.equals("THREE_MONTHS") ? R.color.background : android.R.color.transparent));
        tabThisYear.setBackgroundTintList(getColorStateList(selected.equals("THIS_YEAR") ? R.color.background : android.R.color.transparent));
    }

    private void listenToReportsFromFirestore() {
        db.collection("customers")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    double sumDebts = 0.0;
                    int countAll = 0;
                    int countOverdue = 0;
                    double sumOverdue = 0.0;
                    long now = System.currentTimeMillis();

                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        Customer customer = doc.toObject(Customer.class);
                        if (customer == null || customer.isArchived()) continue;

                        countAll++;
                        double debt = Math.max(0.0, customer.getTotalDebt());
                        sumDebts += debt;

                        long dueDate = customer.getDueDate();
                        if (debt > 0 && dueDate > 0 && now > dueDate) {
                            countOverdue++;
                            sumOverdue += debt;
                        }
                    }

                    totalDebtsBalance = sumDebts;
                    totalCustomers = countAll;
                    overdueCustomers = countOverdue;
                    overdueAmount = sumOverdue;

                    updateStatsUi();
                });
    }

    private void updateStatsUi() {
        if (tvStatNewDebtsAmount != null) {
            tvStatNewDebtsAmount.setText(String.format(Locale.ROOT, "%.0f", totalDebtsBalance));
        }
        if (tvStatNewDebtsCount != null) {
            tvStatNewDebtsCount.setText("إجمالي الزبائن: " + totalCustomers);
        }
        if (tvStatOverdueCount != null) {
            tvStatOverdueCount.setText(String.valueOf(overdueCustomers));
        }
        if (tvStatOverdueTotal != null) {
            tvStatOverdueTotal.setText(String.format(Locale.ROOT, "متأخر: %.0f", overdueAmount));
        }
        if (tvStatNewCustomersCount != null) {
            tvStatNewCustomersCount.setText(String.valueOf(totalCustomers));
        }
    }
}