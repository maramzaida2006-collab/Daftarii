package com.example.daftarii;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CustomerDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private String customerId;
    private String customerPhone = "";
    private double currentDebt = 0.0;

    private FrameLayout btnBackCustomerDetail, btnCustomerMoreOptions;
    private FrameLayout btnEditTx1, btnEditTx2, btnEditTx3;
    private TextView tvCustomerDetailInitials, tvDetailCustomerName, tvDetailCustomerPhone;
    private TextView tvCustomerOverdueStatus, tvDetailDueAmount, tvDueDateAndLastPayment;
    private View badgeCustomerOverdue;

    private LinearLayout btnCallCustomer, btnWhatsappCustomer, btnExportCustomerPdf, btnDueDateReminder;
    private LinearLayout btnDetailAddDebt, btnDetailRecordPayment;
    private TextView tabFilterAll, tabFilterDebts, tabFilterPayments;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        db = FirebaseFirestore.getInstance();
        customerId = getIntent().getStringExtra("customer_id");
        String initialName = getIntent().getStringExtra("customer_name");

        initViews();

        if (initialName != null && !initialName.trim().isEmpty()) {
            setCustomerNameAndInitials(initialName);
        }

        setupListeners();

        if (customerId != null && !customerId.trim().isEmpty()) {
            listenToCustomerDetails();
        }
    }

    private void initViews() {
        btnBackCustomerDetail = findViewById(R.id.btnBackCustomerDetail);
        btnCustomerMoreOptions = findViewById(R.id.btnCustomerMoreOptions);

        tvCustomerDetailInitials = findViewById(R.id.tvCustomerDetailInitials);
        tvDetailCustomerName = findViewById(R.id.tvDetailCustomerName);
        tvDetailCustomerPhone = findViewById(R.id.tvDetailCustomerPhone);
        badgeCustomerOverdue = findViewById(R.id.badgeCustomerOverdue);
        tvCustomerOverdueStatus = findViewById(R.id.tvCustomerOverdueStatus);

        tvDetailDueAmount = findViewById(R.id.tvDetailDueAmount);
        tvDueDateAndLastPayment = findViewById(R.id.tvDueDateAndLastPayment);

        btnCallCustomer = findViewById(R.id.btnCallCustomer);
        btnWhatsappCustomer = findViewById(R.id.btnWhatsappCustomer);
        btnExportCustomerPdf = findViewById(R.id.btnExportCustomerPdf);
        btnDueDateReminder = findViewById(R.id.btnDueDateReminder);

        btnDetailAddDebt = findViewById(R.id.btnDetailAddDebt);
        btnDetailRecordPayment = findViewById(R.id.btnDetailRecordPayment);

        tabFilterAll = findViewById(R.id.tabFilterAll);
        tabFilterDebts = findViewById(R.id.tabFilterDebts);
        tabFilterPayments = findViewById(R.id.tabFilterPayments);

        btnEditTx1 = findViewById(R.id.btnEditTx1);
        btnEditTx2 = findViewById(R.id.btnEditTx2);
        btnEditTx3 = findViewById(R.id.btnEditTx3);
    }

    private void setCustomerNameAndInitials(String name) {
        if (tvDetailCustomerName != null) {
            tvDetailCustomerName.setText(name);
        }
        String trimmed = name.trim();
        if (!trimmed.isEmpty() && tvCustomerDetailInitials != null) {
            String[] parts = trimmed.split("\\s+");
            String initials = parts.length >= 2
                    ? (parts[0].substring(0, 1) + parts[1].substring(0, 1))
                    : trimmed.substring(0, Math.min(2, trimmed.length()));
            tvCustomerDetailInitials.setText(initials);
        }
    }

    private void listenToCustomerDetails() {
        db.collection("customers").document(customerId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null || snapshot == null || !snapshot.exists()) return;

                    Customer customer = snapshot.toObject(Customer.class);
                    if (customer == null) return;

                    if (customer.getName() != null && !customer.getName().trim().isEmpty()) {
                        setCustomerNameAndInitials(customer.getName());
                    }

                    if (tvDetailCustomerPhone != null) {
                        customerPhone = customer.getPhone() != null ? customer.getPhone() : "";
                        tvDetailCustomerPhone.setText(!customerPhone.isEmpty() ? customerPhone : "بدون رقم");
                    }

                    currentDebt = Math.max(0.0, customer.getTotalDebt());
                    if (tvDetailDueAmount != null) {
                        tvDetailDueAmount.setText(String.format(Locale.ROOT, "%.0f", currentDebt));
                    }

                    updateOverdueBadge(currentDebt, customer.getDueDate());
                });
    }

    private void updateOverdueBadge(double debt, long dueDateMillis) {
        if (badgeCustomerOverdue == null || tvCustomerOverdueStatus == null) return;

        long now = System.currentTimeMillis();
        if (debt > 0 && dueDateMillis > 0 && now > dueDateMillis) {
            long diffDays = Math.max(1L, (now - dueDateMillis) / (1000L * 60L * 60L * 24L));
            badgeCustomerOverdue.setVisibility(View.VISIBLE);
            tvCustomerOverdueStatus.setText("متأخر " + diffDays + " أيام");
        } else {
            badgeCustomerOverdue.setVisibility(View.GONE);
        }

        if (tvDueDateAndLastPayment != null && dueDateMillis > 0) {
            String formatted = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    .format(new Date(dueDateMillis));
            tvDueDateAndLastPayment.setText("موعد السداد: " + formatted);
        }
    }

    private void setupListeners() {
        if (btnBackCustomerDetail != null) {
            btnBackCustomerDetail.setOnClickListener(v -> finish());
        }

        if (btnCustomerMoreOptions != null) {
            btnCustomerMoreOptions.setOnClickListener(v ->
                    Toast.makeText(this, "خيارات كشف الحساب", Toast.LENGTH_SHORT).show());
        }

        if (btnCallCustomer != null) {
            btnCallCustomer.setOnClickListener(v -> {
                if (customerPhone == null || customerPhone.trim().isEmpty()) {
                    Toast.makeText(this, "لا يوجد رقم جوال مسجل للزبون", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + customerPhone));
                startActivity(intent);
            });
        }

        if (btnWhatsappCustomer != null) {
            btnWhatsappCustomer.setOnClickListener(v -> {
                if (customerPhone == null || customerPhone.trim().isEmpty()) {
                    Toast.makeText(this, "لا يوجد رقم واتساب متاح للزبون", Toast.LENGTH_SHORT).show();
                    return;
                }
                String cleanPhone = customerPhone.replaceAll("[^0-9]", "");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/" + cleanPhone));
                startActivity(intent);
            });
        }

        if (btnExportCustomerPdf != null) {
            btnExportCustomerPdf.setOnClickListener(v ->
                    Toast.makeText(this, "تصدير كشف الحساب بصيغة PDF", Toast.LENGTH_SHORT).show());
        }

        if (btnDueDateReminder != null) {
            btnDueDateReminder.setOnClickListener(v -> openDatePickerForDueDate());
        }

        if (btnDetailAddDebt != null) {
            btnDetailAddDebt.setOnClickListener(v -> showTransactionDialog(true));
        }

        if (btnDetailRecordPayment != null) {
            btnDetailRecordPayment.setOnClickListener(v -> showTransactionDialog(false));
        }

        if (tabFilterAll != null) {
            tabFilterAll.setOnClickListener(v -> highlightTab("ALL"));
        }
        if (tabFilterDebts != null) {
            tabFilterDebts.setOnClickListener(v -> highlightTab("DEBTS"));
        }
        if (tabFilterPayments != null) {
            tabFilterPayments.setOnClickListener(v -> highlightTab("PAYMENTS"));
        }

        if (btnEditTx1 != null) {
            btnEditTx1.setOnClickListener(v -> showEditTransactionDialog("الحركة 1"));
        }
        if (btnEditTx2 != null) {
            btnEditTx2.setOnClickListener(v -> showEditTransactionDialog("الحركة 2"));
        }
        if (btnEditTx3 != null) {
            btnEditTx3.setOnClickListener(v -> showEditTransactionDialog("الحركة 3"));
        }
    }

    private void highlightTab(String selected) {
        if (tabFilterAll == null || tabFilterDebts == null || tabFilterPayments == null) return;

        tabFilterAll.setTextColor(getColor(selected.equals("ALL") ? R.color.text_primary : R.color.text_secondary));
        tabFilterDebts.setTextColor(getColor(selected.equals("DEBTS") ? R.color.text_primary : R.color.text_secondary));
        tabFilterPayments.setTextColor(getColor(selected.equals("PAYMENTS") ? R.color.text_primary : R.color.text_secondary));

        tabFilterAll.setBackgroundTintList(getColorStateList(selected.equals("ALL") ? R.color.background : android.R.color.transparent));
        tabFilterDebts.setBackgroundTintList(getColorStateList(selected.equals("DEBTS") ? R.color.background : android.R.color.transparent));
        tabFilterPayments.setBackgroundTintList(getColorStateList(selected.equals("PAYMENTS") ? R.color.background : android.R.color.transparent));
    }

    private void showTransactionDialog(boolean isDebt) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(isDebt ? "إضافة دين جديد" : "تسجيل دفعة");

        final EditText inputAmount = new EditText(this);
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        inputAmount.setHint("أدخل المبلغ");
        builder.setView(inputAmount);

        builder.setPositiveButton("حفظ", (dialog, which) -> {
            String raw = inputAmount.getText().toString().trim();
            if (raw.isEmpty()) {
                Toast.makeText(this, "يرجى إدخال المبلغ", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "المبلغ غير صالح", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                Toast.makeText(this, "أدخل مبلغاً أكبر من صفر", Toast.LENGTH_SHORT).show();
                return;
            }

            double delta = isDebt ? amount : -amount;
            saveTransactionToFirestore(isDebt ? "DEBT" : "PAYMENT", amount, delta);
        });

        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void saveTransactionToFirestore(String type, double amount, double debtDelta) {
        if (customerId == null || customerId.trim().isEmpty()) {
            currentDebt = Math.max(0.0, currentDebt + debtDelta);
            if (tvDetailDueAmount != null) {
                tvDetailDueAmount.setText(String.format(Locale.ROOT, "%.0f", currentDebt));
            }
            Toast.makeText(this, "تم تحديث الرصيد محلياً", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference customerRef = db.collection("customers").document(customerId);

        Map<String, Object> txData = new HashMap<>();
        txData.put("customerId", customerId);
        txData.put("type", type);
        txData.put("amount", amount);
        txData.put("timestamp", System.currentTimeMillis());
        txData.put("createdAt", FieldValue.serverTimestamp());

        customerRef.collection("transactions").add(txData);
        customerRef.update("totalDebt", FieldValue.increment(debtDelta))
                .addOnSuccessListener(unused ->
                        Toast.makeText(this, "تم حفظ الحركة وتحديث الرصيد", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "تعذر الحفظ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showEditTransactionDialog(String txLabel) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("تعديل " + txLabel);

        final EditText inputNote = new EditText(this);
        inputNote.setHint("أدخل ملاحظة أو تعديل جديد");
        builder.setView(inputNote);

        builder.setPositiveButton("حفظ التعديل", (dialog, which) ->
                Toast.makeText(this, "تم حفظ تعديل " + txLabel, Toast.LENGTH_SHORT).show());
        builder.setNegativeButton("إلغاء", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void openDatePickerForDueDate() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar chosenCal = Calendar.getInstance();
            chosenCal.set(year, month, dayOfMonth, 23, 59, 59);
            long chosenMillis = chosenCal.getTimeInMillis();

            String chosenDate = String.format(Locale.getDefault(), "%04d/%02d/%02d", year, month + 1, dayOfMonth);
            if (customerId != null && !customerId.trim().isEmpty()) {
                db.collection("customers").document(customerId)
                        .update("dueDate", chosenMillis);
            }
            if (tvDueDateAndLastPayment != null) {
                tvDueDateAndLastPayment.setText("موعد السداد: " + chosenDate);
            }
            Toast.makeText(this, "تم تحديد الموعد: " + chosenDate, Toast.LENGTH_SHORT).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }
}