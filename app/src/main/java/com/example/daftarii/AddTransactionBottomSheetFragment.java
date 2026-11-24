package com.example.daftarii;

import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AddTransactionBottomSheetFragment extends BottomSheetDialogFragment {

    public static final String ARG_IS_DEBT = "arg_is_debt";
    public static final String ARG_PRESELECTED_ID = "arg_preselected_id";
    public static final String ARG_CUSTOMER_IDS = "arg_customer_ids";
    public static final String ARG_CUSTOMER_NAMES = "arg_customer_names";
    public static final String ARG_CUSTOMER_DEBTS = "arg_customer_debts";
    public static final String ARG_CUSTOMER_DUE_DATES = "arg_customer_due_dates";

    private boolean isDebt = true;
    private String preselectedCustomerId = "";
    private Customer selectedCustomer;
    private final List<Customer> customers = new ArrayList<>();

    private long selectedTxTimestamp;
    private long selectedDueDateMillis;

    private TextView tvAvatar, tvCustomerInfo, btnCancel;
    private LinearLayout btnDebt, btnPayment;
    private ImageView ivDebt, ivPayment;
    private TextView tvDebtLabel, tvPaymentLabel;
    private EditText etAmount, etDescription;
    private TextView chip5, chip10, chip20, chip50;
    private LinearLayout fieldDate, fieldDueDate, balanceBar, btnSave;
    private TextView tvDate, tvDueDate, tvBalanceAfter;

    private FirebaseFirestore db;

    public static AddTransactionBottomSheetFragment newInstance(boolean isDebt, List<Customer> customerList) {
        return newInstance(isDebt, customerList, null);
    }

    public static AddTransactionBottomSheetFragment newInstance(boolean isDebt, List<Customer> customerList, @Nullable String preselectedId) {
        AddTransactionBottomSheetFragment fragment = new AddTransactionBottomSheetFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_DEBT, isDebt);
        args.putString(ARG_PRESELECTED_ID, preselectedId != null ? preselectedId : "");

        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
        double[] debts = new double[customerList.size()];
        long[] dueDates = new long[customerList.size()];

        for (int i = 0; i < customerList.size(); i++) {
            Customer c = customerList.get(i);
            ids.add(c.getId() != null ? c.getId() : "");
            names.add(c.getName() != null ? c.getName() : "");
            debts[i] = c.getTotalDebt();
            dueDates[i] = c.getDueDate();
        }

        args.putStringArrayList(ARG_CUSTOMER_IDS, ids);
        args.putStringArrayList(ARG_CUSTOMER_NAMES, names);
        args.putDoubleArray(ARG_CUSTOMER_DEBTS, debts);
        args.putLongArray(ARG_CUSTOMER_DUE_DATES, dueDates);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        selectedTxTimestamp = System.currentTimeMillis();

        Bundle args = getArguments();
        if (args != null) {
            isDebt = args.getBoolean(ARG_IS_DEBT, true);
            preselectedCustomerId = args.getString(ARG_PRESELECTED_ID, "");

            ArrayList<String> ids = args.getStringArrayList(ARG_CUSTOMER_IDS);
            ArrayList<String> names = args.getStringArrayList(ARG_CUSTOMER_NAMES);
            double[] debts = args.getDoubleArray(ARG_CUSTOMER_DEBTS);
            long[] dueDates = args.getLongArray(ARG_CUSTOMER_DUE_DATES);

            int size = names != null ? names.size() : 0;
            for (int i = 0; i < size; i++) {
                Customer c = new Customer();
                c.setId(ids != null && i < ids.size() ? ids.get(i) : "");
                c.setName(names.get(i));
                c.setTotalDebt(debts != null && i < debts.length ? debts[i] : 0.0);
                c.setDueDate(dueDates != null && i < dueDates.length ? dueDates[i] : 0L);
                customers.add(c);
                if (!preselectedCustomerId.isEmpty() && preselectedCustomerId.equals(c.getId())) {
                    selectedCustomer = c;
                }
            }
        }

        if (selectedCustomer == null && !customers.isEmpty()) {
            selectedCustomer = customers.get(0);
        }

        selectedDueDateMillis = selectedCustomer != null ? selectedCustomer.getDueDate() : 0L;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_transaction_bottom_sheet, container, false);

        bindViews(view);
        bindCustomerInfo();
        applyTransactionMode(isDebt);
        updateDateTexts();
        recalculateBalanceAfter();
        setupListeners();

        return view;
    }

    private void bindViews(View view) {
        tvAvatar = view.findViewById(R.id.tvAvatar);
        tvCustomerInfo = view.findViewById(R.id.tvCustomerInfo);
        btnCancel = view.findViewById(R.id.btnCancel);

        btnDebt = view.findViewById(R.id.btnDebt);
        btnPayment = view.findViewById(R.id.btnPayment);
        ivDebt = view.findViewById(R.id.ivDebt);
        ivPayment = view.findViewById(R.id.ivPayment);
        tvDebtLabel = view.findViewById(R.id.tvDebtLabel);
        tvPaymentLabel = view.findViewById(R.id.tvPaymentLabel);

        etAmount = view.findViewById(R.id.etAmount);
        etDescription = view.findViewById(R.id.etDescription);

        chip5 = view.findViewById(R.id.chip5);
        chip10 = view.findViewById(R.id.chip10);
        chip20 = view.findViewById(R.id.chip20);
        chip50 = view.findViewById(R.id.chip50);

        fieldDate = view.findViewById(R.id.fieldDate);
        fieldDueDate = view.findViewById(R.id.fieldDueDate);
        tvDate = view.findViewById(R.id.tvDate);
        tvDueDate = view.findViewById(R.id.tvDueDate);

        balanceBar = view.findViewById(R.id.balanceBar);
        tvBalanceAfter = view.findViewById(R.id.tvBalanceAfter);
        btnSave = view.findViewById(R.id.btnSave);
    }

    private void bindCustomerInfo() {
        if (selectedCustomer == null) return;

        String name = selectedCustomer.getName() != null ? selectedCustomer.getName().trim() : "";
        tvAvatar.setText(buildInitials(name));
        tvCustomerInfo.setText(String.format(Locale.getDefault(), "%s · الرصيد ₪ %.2f", name, selectedCustomer.getTotalDebt()));
        selectedDueDateMillis = selectedCustomer.getDueDate();
        updateDateTexts();
    }

    private String buildInitials(String name) {
        if (name == null || name.trim().isEmpty()) return "ز";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return "" + parts[0].charAt(0) + parts[1].charAt(0);
        }
        return name.trim().substring(0, Math.min(2, name.trim().length()));
    }

    private void applyTransactionMode(boolean debtMode) {
        this.isDebt = debtMode;

        int redColor = Color.parseColor("#C0392B");
        int darkRedText = Color.parseColor("#7A3A33");
        int greenColor = Color.parseColor("#1E8449");
        int darkGreenText = Color.parseColor("#145A32");
        int mutedColor = Color.parseColor("#6B6B6B");

        if (isDebt) {
            btnDebt.setBackgroundResource(R.drawable.bg_toggle_debt_selected);
            btnPayment.setBackground(null);
            ivDebt.setColorFilter(redColor);
            tvDebtLabel.setTextColor(redColor);
            ivPayment.setColorFilter(mutedColor);
            tvPaymentLabel.setTextColor(mutedColor);

            balanceBar.setBackgroundResource(R.drawable.bg_balance_after_red);
            tvBalanceAfter.setTextColor(redColor);
            if (balanceBar.getChildCount() >= 3 && balanceBar.getChildAt(2) instanceof TextView) {
                ((TextView) balanceBar.getChildAt(2)).setTextColor(darkRedText);
            }
        } else {
            btnPayment.setBackgroundResource(R.drawable.bg_toggle_debt_selected);
            btnDebt.setBackground(null);
            ivPayment.setColorFilter(greenColor);
            tvPaymentLabel.setTextColor(greenColor);
            ivDebt.setColorFilter(mutedColor);
            tvDebtLabel.setTextColor(mutedColor);

            tvBalanceAfter.setTextColor(greenColor);
            if (balanceBar.getChildCount() >= 3 && balanceBar.getChildAt(2) instanceof TextView) {
                ((TextView) balanceBar.getChildAt(2)).setTextColor(darkGreenText);
            }
        }

        recalculateBalanceAfter();
    }

    private void setupListeners() {
        btnCancel.setOnClickListener(v -> dismiss());

        if (customers.size() > 1) {
            tvCustomerInfo.setOnClickListener(v -> showCustomerPickerDialog());
        }

        btnDebt.setOnClickListener(v -> applyTransactionMode(true));
        btnPayment.setOnClickListener(v -> applyTransactionMode(false));

        chip5.setOnClickListener(v -> addQuickAmount(5.0));
        chip10.setOnClickListener(v -> addQuickAmount(10.0));
        chip20.setOnClickListener(v -> addQuickAmount(20.0));
        chip50.setOnClickListener(v -> addQuickAmount(50.0));

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateBalanceAfter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fieldDate.setOnClickListener(v -> openDatePicker(false));
        fieldDueDate.setOnClickListener(v -> openDatePicker(true));

        btnSave.setOnClickListener(v -> validateAndSaveTransaction());
    }

    private void showCustomerPickerDialog() {
        String[] names = new String[customers.size()];
        for (int i = 0; i < customers.size(); i++) {
            names[i] = customers.get(i).getName() != null ? customers.get(i).getName() : "";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("اختر الزبون")
                .setItems(names, (dialog, which) -> {
                    selectedCustomer = customers.get(which);
                    bindCustomerInfo();
                    recalculateBalanceAfter();
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void addQuickAmount(double increment) {
        String currentStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        Double currentVal = parseDoubleSafe(currentStr);
        double nextVal = (currentVal != null ? currentVal : 0.0) + increment;
        nextVal = Math.round(nextVal * 100.0) / 100.0;
        etAmount.setText(String.format(Locale.US, "%.2f", nextVal));
        etAmount.setSelection(etAmount.getText().length());
    }

    private void openDatePicker(boolean forDueDate) {
        Calendar cal = Calendar.getInstance();
        long initialMillis = forDueDate
                ? (selectedDueDateMillis > 0 ? selectedDueDateMillis : System.currentTimeMillis())
                : selectedTxTimestamp;
        cal.setTimeInMillis(initialMillis);

        new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 12, 0, 0);
                    chosen.set(Calendar.MILLISECOND, 0);

                    if (forDueDate) {
                        selectedDueDateMillis = chosen.getTimeInMillis();
                    } else {
                        selectedTxTimestamp = chosen.getTimeInMillis();
                    }
                    updateDateTexts();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void updateDateTexts() {
        SimpleDateFormat txFormat = new SimpleDateFormat("dd MMM · hh:mm a", new Locale("ar"));
        tvDate.setText(txFormat.format(new Date(selectedTxTimestamp)));

        if (selectedDueDateMillis > 0) {
            SimpleDateFormat dueFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
            tvDueDate.setText(dueFormat.format(new Date(selectedDueDateMillis)));
        } else {
            tvDueDate.setText("غير محدد");
        }
    }

    private void recalculateBalanceAfter() {
        double currentDebt = selectedCustomer != null ? selectedCustomer.getTotalDebt() : 0.0;
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        Double parsed = parseDoubleSafe(amountStr);
        double entered = parsed != null ? parsed : 0.0;

        double signedChange = isDebt ? entered : -entered;
        double afterBalance = Math.round((currentDebt + signedChange) * 100.0) / 100.0;
        tvBalanceAfter.setText(String.format(Locale.getDefault(), "₪ %.2f", afterBalance));
    }

    private void validateAndSaveTransaction() {
        if (selectedCustomer == null || selectedCustomer.getId() == null || selectedCustomer.getId().isEmpty()) {
            Toast.makeText(requireContext(), "يرجى اختيار زبون أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";
        Double parsedAmount = parseDoubleSafe(amountStr);
        if (parsedAmount == null || parsedAmount <= 0) {
            etAmount.setError("أدخل مبلغاً صحيحاً أكبر من صفر");
            etAmount.requestFocus();
            return;
        }

        String desc = etDescription.getText() != null ? etDescription.getText().toString().trim() : "";
        if (desc.isEmpty()) {
            desc = isDebt ? "دين" : "سداد دفعة نقدية";
        }

        double signedAmount = isDebt ? parsedAmount : -parsedAmount;
        double updatedTotalDebt = Math.round((selectedCustomer.getTotalDebt() + signedAmount) * 100.0) / 100.0;

        btnSave.setEnabled(false);

        DocumentReference customerRef = db.collection("customers").document(selectedCustomer.getId());
        DocumentReference debtRef = customerRef.collection("debts").document();

        Map<String, Object> customerUpdates = new HashMap<>();
        customerUpdates.put("totalDebt", updatedTotalDebt);
        customerUpdates.put("dueDate", selectedDueDateMillis);

        Map<String, Object> debtMap = new HashMap<>();
        debtMap.put("id", debtRef.getId());
        debtMap.put("customerId", selectedCustomer.getId());
        debtMap.put("itemName", desc);
        debtMap.put("amount", signedAmount);
        debtMap.put("timestamp", selectedTxTimestamp);
        debtMap.put("dueDate", selectedDueDateMillis);
        debtMap.put("archived", false);

        WriteBatch batch = db.batch();
        batch.update(customerRef, customerUpdates);
        batch.set(debtRef, debtMap);

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "تم حفظ الحركة وتحديث الرصيد في Firestore", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), "تم الحفظ محلياً وسيتم المزامنة تلقائياً", Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                });
    }

    private Double parseDoubleSafe(String value) {
        if (value == null || value.isEmpty() || value.equals(".") || value.equals("-") || value.equals("-.")) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (Double.isNaN(parsed) || Double.isInfinite(parsed)) return null;
            return parsed;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}