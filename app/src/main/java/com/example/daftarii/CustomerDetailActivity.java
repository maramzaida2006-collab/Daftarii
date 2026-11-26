package com.example.daftarii;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CustomerDetailActivity extends AppCompatActivity implements EditTransactionSheet.Host {

    private static final String TAG = "CustomerDetailActivity";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_DEBTS = 1;
    private static final int FILTER_PAYMENTS = 2;

    private ImageView btnBack, btnDownloadPdf;
    private FrameLayout btnBackCustomerDetail, btnCustomerMoreOptions;
    private TextView tvDetailCustomerName, tvDetailCustomerPhone, tvDetailTotalDebt;
    private TextView tvCustomerDetailInitials, tvDueDateAndLastPayment;
    private TextInputEditText etAmount, etItemName;
    private LinearLayout btnAddDebtItem, btnPayDebt;
    private LinearLayout btnCallCustomer, btnWhatsappCustomer, btnExportCustomerPdf, btnDueDateReminder;
    private TextView tabFilterAll, tabFilterDebts, tabFilterPayments;
    private RecyclerView recyclerViewDebts;

    private FirebaseFirestore db;
    private String customerId, customerName, customerPhone;
    private double currentTotalDebt = 0.0;
    private long currentDueDateMillis = 0L;
    private int currentFilterMode = FILTER_ALL;

    private final List<DebtItem> allDebtItems = new ArrayList<>();
    private final List<DebtItem> debtItemList = new ArrayList<>();
    private DebtDetailsAdapter debtsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_detail);

        db = FirebaseFirestore.getInstance();

        customerId = getIntent().getStringExtra("CUSTOMER_ID");
        customerName = getIntent().getStringExtra("CUSTOMER_NAME");
        customerPhone = getIntent().getStringExtra("CUSTOMER_PHONE");
        currentTotalDebt = getIntent().getDoubleExtra("CUSTOMER_TOTAL_DEBT", 0.0);
        currentDueDateMillis = getIntent().getLongExtra("CUSTOMER_DUE_DATE", 0L);

        bindViews();
        bindInitialCustomerHeader();
        setupRecyclerView();
        setupClickListeners();
        applyFilterTab(FILTER_ALL);

        loadCustomerDetailsAndDebts();
    }

    private void bindViews() {
        btnBack = findViewById(R.id.btnBack);
        btnBackCustomerDetail = findViewById(R.id.btnBackCustomerDetail);
        btnCustomerMoreOptions = findViewById(R.id.btnCustomerMoreOptions);

        tvCustomerDetailInitials = findViewById(R.id.tvCustomerDetailInitials);
        tvDetailCustomerName = findViewById(R.id.tvDetailCustomerName);
        tvDetailCustomerPhone = findViewById(R.id.tvDetailCustomerPhone);
        tvDetailTotalDebt = findViewById(R.id.tvDetailTotalDebt);
        tvDueDateAndLastPayment = findViewById(R.id.tvDueDateAndLastPayment);

        btnCallCustomer = findViewById(R.id.btnCallCustomer);
        btnWhatsappCustomer = findViewById(R.id.btnWhatsappCustomer);
        btnExportCustomerPdf = findViewById(R.id.btnExportCustomerPdf);
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf);
        btnDueDateReminder = findViewById(R.id.btnDueDateReminder);

        btnAddDebtItem = findViewById(R.id.btnAddDebtItem);
        btnPayDebt = findViewById(R.id.btnPayDebt);

        etAmount = findViewById(R.id.etAmount);
        etItemName = findViewById(R.id.etItemName);

        tabFilterAll = findViewById(R.id.tabFilterAll);
        tabFilterDebts = findViewById(R.id.tabFilterDebts);
        tabFilterPayments = findViewById(R.id.tabFilterPayments);

        recyclerViewDebts = findViewById(R.id.recyclerViewDebts);
    }

    private void bindInitialCustomerHeader() {
        String safeName = (customerName != null && !customerName.trim().isEmpty())
                ? customerName.trim()
                : getString(R.string.notif_unknown_customer);
        tvDetailCustomerName.setText(safeName);
        tvCustomerDetailInitials.setText(buildInitials(safeName));

        tvDetailCustomerPhone.setText(
                (customerPhone != null && !customerPhone.trim().isEmpty())
                        ? customerPhone.trim()
                        : "0590000000"
        );

        tvDetailTotalDebt.setText("₪ " + formatAmount(currentTotalDebt));
        updateDueDateLabel();
    }

    private String buildInitials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return "" + parts[0].charAt(0) + parts[1].charAt(0);
        }
        return name.substring(0, Math.min(2, name.length()));
    }

    private String formatAmount(double amount) {
        return String.format(Locale.US, "%.2f", amount);
    }

    private void updateDueDateLabel() {
        if (tvDueDateAndLastPayment == null) return;
        if (currentDueDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", new Locale("ar"));
            tvDueDateAndLastPayment.setText(getString(R.string.detail_due_date, sdf.format(new Date(currentDueDateMillis))));
        } else {
            tvDueDateAndLastPayment.setText(getString(R.string.detail_due_date, getString(R.string.due_not_set)));
        }
    }

    private void setupRecyclerView() {
        debtsAdapter = new DebtDetailsAdapter(debtItemList, new DebtDetailsAdapter.OnDebtClickListener() {
            @Override
            public void onEditClick(DebtItem debtItem) {
                showEditDebtDialog(debtItem);
            }

            @Override
            public void onDeleteClick(DebtItem debtItem) {
                showDeleteConfirmationDialog(debtItem);
            }
        });

        recyclerViewDebts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewDebts.setAdapter(debtsAdapter);
    }

    private void setupClickListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
        if (btnBackCustomerDetail != null) {
            btnBackCustomerDetail.setOnClickListener(v -> finish());
        }
        if (btnCustomerMoreOptions != null) {
            btnCustomerMoreOptions.setOnClickListener(v -> showMoreOptionsDialog());
        }

        if (btnCallCustomer != null) {
            btnCallCustomer.setOnClickListener(v -> callCustomer());
        }
        if (btnWhatsappCustomer != null) {
            btnWhatsappCustomer.setOnClickListener(v -> openWhatsappForCustomer());
        }
        if (btnExportCustomerPdf != null) {
            btnExportCustomerPdf.setOnClickListener(v -> generateAndSharePdf());
        } else if (btnDownloadPdf != null) {
            btnDownloadPdf.setOnClickListener(v -> generateAndSharePdf());
        }
        if (btnDueDateReminder != null) {
            btnDueDateReminder.setOnClickListener(v -> showDueDatePicker());
        }

        if (btnAddDebtItem != null) {
            btnAddDebtItem.setOnClickListener(v -> addNewDebtItem());
        }
        if (btnPayDebt != null) {
            btnPayDebt.setOnClickListener(v -> showPaymentDialog());
        }

        if (tabFilterAll != null) {
            tabFilterAll.setOnClickListener(v -> applyFilterTab(FILTER_ALL));
        }
        if (tabFilterDebts != null) {
            tabFilterDebts.setOnClickListener(v -> applyFilterTab(FILTER_DEBTS));
        }
        if (tabFilterPayments != null) {
            tabFilterPayments.setOnClickListener(v -> applyFilterTab(FILTER_PAYMENTS));
        }
    }

    private void applyFilterTab(int mode) {
        currentFilterMode = mode;

        int activeBg = ContextCompat.getColor(this, R.color.background);
        int activeText = ContextCompat.getColor(this, R.color.text_primary);
        int inactiveText = ContextCompat.getColor(this, R.color.text_secondary);

        if (tabFilterAll != null) {
            boolean selected = mode == FILTER_ALL;
            tabFilterAll.setBackgroundResource(selected ? R.drawable.shape_r16 : 0);
            if (selected) tabFilterAll.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.background));
            tabFilterAll.setTextColor(selected ? activeText : inactiveText);
        }
        if (tabFilterDebts != null) {
            boolean selected = mode == FILTER_DEBTS;
            tabFilterDebts.setBackgroundResource(selected ? R.drawable.shape_r16 : 0);
            if (selected) tabFilterDebts.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.background));
            tabFilterDebts.setTextColor(selected ? activeText : inactiveText);
        }
        if (tabFilterPayments != null) {
            boolean selected = mode == FILTER_PAYMENTS;
            tabFilterPayments.setBackgroundResource(selected ? R.drawable.shape_r16 : 0);
            if (selected) tabFilterPayments.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.background));
            tabFilterPayments.setTextColor(selected ? activeText : inactiveText);
        }

        refreshFilteredList();
    }

    private void refreshFilteredList() {
        debtItemList.clear();
        for (DebtItem item : allDebtItems) {
            if (currentFilterMode == FILTER_DEBTS) {
                if (item.getAmount() > 0) {
                    debtItemList.add(item);
                }
            } else if (currentFilterMode == FILTER_PAYMENTS) {
                if (item.getAmount() < 0) {
                    debtItemList.add(item);
                }
            } else {
                debtItemList.add(item);
            }
        }
        if (debtsAdapter != null) {
            debtsAdapter.notifyDataSetChanged();
        }
    }

    private void callCustomer() {
        String phone = customerPhone != null ? customerPhone.trim() : "";
        if (phone.isEmpty()) {
            Toast.makeText(this, getString(R.string.notif_no_phone), Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    private void openWhatsappForCustomer() {
        String rawPhone = customerPhone != null ? customerPhone.trim().replaceAll("[^0-9]", "") : "";
        if (rawPhone.isEmpty()) {
            Toast.makeText(this, getString(R.string.notif_no_phone), Toast.LENGTH_SHORT).show();
            return;
        }

        if (rawPhone.startsWith("05")) {
            rawPhone = "972" + rawPhone.substring(1);
        } else if (rawPhone.startsWith("0")) {
            rawPhone = "970" + rawPhone.substring(1);
        }

        String name = (customerName != null && !customerName.trim().isEmpty()) ? customerName.trim() : "";
        String msg = getString(R.string.detail_whatsapp_message, name, formatAmount(currentTotalDebt));

        try {
            String url = "https://wa.me/" + rawPhone + "?text=" + Uri.encode(msg);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.notif_no_whatsapp), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDueDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (currentDueDateMillis > 0) {
            cal.setTimeInMillis(currentDueDateMillis);
        }

        new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 12, 0, 0);
                    chosen.set(Calendar.MILLISECOND, 0);
                    currentDueDateMillis = chosen.getTimeInMillis();
                    updateDueDateLabel();

                    if (customerId != null && !customerId.isEmpty()) {
                        db.collection("customers").document(customerId)
                                .update("dueDate", currentDueDateMillis)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(this, getString(R.string.detail_due_updated), Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, getString(R.string.saved_offline), Toast.LENGTH_SHORT).show());
                    }
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showMoreOptionsDialog() {
        String[] options = {getString(R.string.detail_option_due), getString(R.string.detail_option_pdf), getString(R.string.detail_option_archive)};
        new AlertDialog.Builder(this)
                .setTitle(customerName != null ? customerName : getString(R.string.detail_options_title))
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        showDueDatePicker();
                    } else if (which == 1) {
                        generateAndSharePdf();
                    } else if (which == 2) {
                        confirmArchiveCustomer();
                    }
                })
                .setNegativeButton(R.string.action_close, null)
                .show();
    }

    private void confirmArchiveCustomer() {
        if (customerId == null || customerId.isEmpty()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.detail_archive_customer_title)
                .setMessage(R.string.detail_archive_customer_message)
                .setPositiveButton(R.string.action_archive, (dialog, which) -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("archived", true);
                    updates.put("deletedAt", System.currentTimeMillis());

                    db.collection("customers").document(customerId)
                            .update(updates)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, getString(R.string.detail_customer_archived), Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, getString(R.string.archive_failed), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty() || value.equals(".") || value.equals("-") || value.equals("-.")) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void loadCustomerDetailsAndDebts() {
        if (customerId == null) return;

        db.collection("customers").document(customerId)
                .addSnapshotListener((docSnap, docErr) -> {
                    if (docErr == null && docSnap != null && docSnap.exists()) {
                        Long due = docSnap.getLong("dueDate");
                        if (due != null && due > 0) {
                            currentDueDateMillis = due;
                            updateDueDateLabel();
                        }
                    }
                });

        db.collection("customers").document(customerId)
                .collection("debts")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "خطأ في جلب الحركات: " + error.getMessage());
                        return;
                    }

                    if (value == null) {
                        return;
                    }

                    allDebtItems.clear();
                    double calculatedTotal = 0.0;

                    for (QueryDocumentSnapshot doc : value) {
                        Boolean archived = doc.getBoolean("archived");
                        if (archived != null && archived) {
                            continue;
                        }

                        DebtItem item = doc.toObject(DebtItem.class);
                        if (item == null) continue;
                        item.setId(doc.getId());
                        item.setCustomerId(customerId);
                        allDebtItems.add(item);

                        calculatedTotal += item.getAmount();
                    }

                    allDebtItems.sort((item1, item2) -> Long.compare(item2.getTimestamp(), item1.getTimestamp()));

                    currentTotalDebt = Math.round(calculatedTotal * 100.0) / 100.0;
                    tvDetailTotalDebt.setText("₪ " + formatAmount(currentTotalDebt));

                    refreshFilteredList();

                    db.collection("customers").document(customerId)
                            .update("totalDebt", currentTotalDebt)
                            .addOnFailureListener(e -> Log.e(TAG, "فشل التحديث المحلي: " + e.getMessage()));
                });
    }

    private void addNewDebtItem() {
        if (customerId == null) {
            Toast.makeText(this, getString(R.string.archive_customer_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        String itemName = etItemName.getText() != null ? etItemName.getText().toString().trim() : "";

        if (amountStr.isEmpty() || itemName.isEmpty()) {
            Toast.makeText(this, getString(R.string.detail_enter_amount_and_item), Toast.LENGTH_SHORT).show();
            return;
        }

        Double parsedAmount = parseDoubleSafe(amountStr);
        if (parsedAmount == null || parsedAmount <= 0) {
            Toast.makeText(this, getString(R.string.error_amount_not_positive), Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Math.abs(parsedAmount);
        long currentTime = System.currentTimeMillis();

        currentTotalDebt = Math.round((currentTotalDebt + amount) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + formatAmount(currentTotalDebt));

        String debtId = db.collection("customers").document(customerId)
                .collection("debts").document().getId();

        DebtItem debtItem = new DebtItem(debtId, itemName, amount, currentTime, currentTime);
        debtItem.setCustomerId(customerId);

        allDebtItems.add(0, debtItem);
        refreshFilteredList();

        etAmount.setText("");
        etItemName.setText("");
        etAmount.requestFocus();

        Map<String, Object> debtMap = new HashMap<>();
        debtMap.put("id", debtId);
        debtMap.put("customerId", customerId);
        debtMap.put("itemName", itemName);
        debtMap.put("type", "DEBT");
        debtMap.put("amount", amount);
        debtMap.put("timestamp", currentTime);
        debtMap.put("dueDate", currentDueDateMillis > 0 ? currentDueDateMillis : currentTime);
        debtMap.put("archived", false);

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(debtId);

        batch.update(customerRef, "totalDebt", currentTotalDebt);
        batch.set(debtRef, debtMap);

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, getString(R.string.tx_saved), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.saved_offline), Toast.LENGTH_SHORT).show());
    }

    private void showPaymentDialog() {
        if (customerId == null) {
            Toast.makeText(this, getString(R.string.archive_customer_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.detail_payment_title);

        final TextInputEditText inputAmount = new TextInputEditText(this);
        inputAmount.setHint(R.string.detail_payment_hint);
        inputAmount.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);
        layout.addView(inputAmount);

        builder.setView(layout);

        builder.setPositiveButton(R.string.detail_payment_confirm, (dialog, which) -> {
            String amountStr = inputAmount.getText() != null ? inputAmount.getText().toString().trim() : "";
            if (amountStr.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_amount_empty), Toast.LENGTH_SHORT).show();
                return;
            }

            Double parsedPayment = parseDoubleSafe(amountStr);
            if (parsedPayment == null || parsedPayment <= 0) {
                Toast.makeText(this, getString(R.string.error_amount_not_positive), Toast.LENGTH_SHORT).show();
                return;
            }

            processPaymentTransaction(Math.abs(parsedPayment));
        });

        builder.setNegativeButton(R.string.action_cancel, null);
        builder.show();
    }

    private void processPaymentTransaction(double paymentAmount) {
        currentTotalDebt = Math.round((currentTotalDebt - paymentAmount) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + formatAmount(currentTotalDebt));

        String debtId = db.collection("customers").document(customerId)
                .collection("debts").document().getId();

        long currentTime = System.currentTimeMillis();
        DebtItem paymentItem = new DebtItem(debtId, getString(R.string.tx_default_payment_note), -paymentAmount, currentTime, currentTime);
        paymentItem.setCustomerId(customerId);

        allDebtItems.add(0, paymentItem);
        refreshFilteredList();

        Map<String, Object> debtMap = new HashMap<>();
        debtMap.put("id", debtId);
        debtMap.put("customerId", customerId);
        debtMap.put("itemName", getString(R.string.tx_default_payment_note));
        debtMap.put("type", "PAYMENT");
        debtMap.put("amount", -paymentAmount);
        debtMap.put("timestamp", currentTime);
        debtMap.put("dueDate", currentDueDateMillis > 0 ? currentDueDateMillis : currentTime);
        debtMap.put("archived", false);

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(debtId);

        batch.update(customerRef, "totalDebt", currentTotalDebt);
        batch.set(debtRef, debtMap);

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, getString(R.string.detail_payment_saved), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.saved_offline), Toast.LENGTH_SHORT).show());
    }

    /**
     * تعديل حركة: Bottom Sheet حسب التصميم (EditTransactionSheet)، مع سجل التعديلات.
     * الحفظ بـ Firestore بيصير جوا الـ Sheet (TransactionEditor)، وهون بس بنحدّث الشاشة.
     */
    private void showEditDebtDialog(DebtItem debtItem) {
        if (customerId == null || debtItem == null || debtItem.getId() == null) return;
        EditTransactionSheet.newInstance(customerId, customerName, debtItem)
                .show(getSupportFragmentManager(), "edit_transaction");
    }

    @Override
    public void onTransactionEdited(String debtId, String newNote, double newSignedAmount, double balanceDiff) {
        DebtItem item = findDebt(debtId);
        if (item != null) {
            item.setItemName(newNote);
            item.setAmount(newSignedAmount);
        }
        currentTotalDebt = Math.round((currentTotalDebt + balanceDiff) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + formatAmount(currentTotalDebt));
        refreshFilteredList();
    }

    @Override
    public void onArchiveRequested(String debtId) {
        DebtItem item = findDebt(debtId);
        if (item != null) showDeleteConfirmationDialog(item);
    }

    private DebtItem findDebt(String debtId) {
        if (debtId == null) return null;
        for (DebtItem d : allDebtItems) {
            if (debtId.equals(d.getId())) return d;
        }
        return null;
    }

    private void showDeleteConfirmationDialog(DebtItem debtItem) {
        if (debtItem == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.detail_archive_tx_title)
                .setMessage(getString(R.string.detail_archive_tx_message, debtItem.getItemName()))
                .setPositiveButton(R.string.action_archive, (dialog, which) -> executeDeleteDebtItem(debtItem))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void executeDeleteDebtItem(DebtItem item) {
        if (customerId == null || item == null || item.getId() == null) {
            Toast.makeText(this, getString(R.string.archive_tx_not_found), Toast.LENGTH_SHORT).show();
            return;
        }

        currentTotalDebt = Math.round((currentTotalDebt - item.getAmount()) * 100.0) / 100.0;
        tvDetailTotalDebt.setText("₪ " + formatAmount(currentTotalDebt));

        allDebtItems.remove(item);
        refreshFilteredList();

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        DocumentReference debtRef = customerRef.collection("debts").document(item.getId());

        batch.update(customerRef, "totalDebt", currentTotalDebt);
        batch.update(debtRef, "archived", true, "deletedAt", System.currentTimeMillis());

        batch.commit()
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, getString(R.string.detail_tx_archived), Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.archive_failed), Toast.LENGTH_SHORT).show());
    }

    private void generateAndSharePdf() {
        openStatementPreview();
    }

    private void openStatementPreview() {
        Intent intent = new Intent(CustomerDetailActivity.this, StatementPreviewActivity.class);
        intent.putExtra(StatementPreviewActivity.EXTRA_CUSTOMER_ID, customerId);
        intent.putExtra(StatementPreviewActivity.EXTRA_CUSTOMER_NAME, customerName);
        intent.putExtra(StatementPreviewActivity.EXTRA_CUSTOMER_PHONE, customerPhone);
        intent.putExtra(StatementPreviewActivity.EXTRA_TOTAL_DEBT, currentTotalDebt);
        startActivity(intent);
    }
}