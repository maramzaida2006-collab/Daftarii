package com.example.daftarii;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
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

public class NewCustomerBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_EXISTING_NAMES = "arg_existing_names";

    private TextView btnCancel, tvDueHint;
    private EditText etName, etPhone, etAmount;
    private ChipGroup chipGroupDue;
    private Chip chipNone, chipWeek, chipTwoWeeks, chipMonth, chipCustom;
    private MaterialButton btnSave;

    private long selectedDueDateMillis = 0L;
    private final List<String> existingCustomerNames = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    public static NewCustomerBottomSheetFragment newInstance(List<Customer> customers) {
        NewCustomerBottomSheetFragment fragment = new NewCustomerBottomSheetFragment();
        ArrayList<String> names = new ArrayList<>();
        if (customers != null) {
            for (Customer c : customers) {
                if (c != null && c.getName() != null) {
                    names.add(c.getName().trim());
                }
            }
        }
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_EXISTING_NAMES, names);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            ArrayList<String> names = getArguments().getStringArrayList(ARG_EXISTING_NAMES);
            if (names != null) {
                existingCustomerNames.addAll(names);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_new_customer_bottom_sheet, container, false);

        btnCancel = view.findViewById(R.id.btnCancel);
        tvDueHint = view.findViewById(R.id.tvDueHint);
        etName = view.findViewById(R.id.etName);
        etPhone = view.findViewById(R.id.etPhone);
        etAmount = view.findViewById(R.id.etAmount);
        chipGroupDue = view.findViewById(R.id.chipGroupDue);
        chipNone = view.findViewById(R.id.chipNone);
        chipWeek = view.findViewById(R.id.chipWeek);
        chipTwoWeeks = view.findViewById(R.id.chipTwoWeeks);
        chipMonth = view.findViewById(R.id.chipMonth);
        chipCustom = view.findViewById(R.id.chipCustom);
        btnSave = view.findViewById(R.id.btnSave);

        btnCancel.setOnClickListener(v -> dismiss());

        // الافتراضي في الـ XML هو chipWeek (أسبوع)
        applyPresetDueDays(7);

        chipGroupDue.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);

            if (checkedId == R.id.chipNone) {
                selectedDueDateMillis = 0L;
                tvDueHint.setText(R.string.customer_due_none);
            } else if (checkedId == R.id.chipWeek) {
                applyPresetDueDays(7);
            } else if (checkedId == R.id.chipTwoWeeks) {
                applyPresetDueDays(14);
            } else if (checkedId == R.id.chipMonth) {
                applyPresetDueDays(30);
            } else if (checkedId == R.id.chipCustom) {
                openCustomDatePicker();
            }
        });

        btnSave.setOnClickListener(v -> validateAndSaveCustomer());

        return view;
    }

    private void applyPresetDueDays(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, days);
        selectedDueDateMillis = cal.getTimeInMillis();
        updateDueHintText(selectedDueDateMillis);
    }

    private void openCustomDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedDueDateMillis > 0) {
            cal.setTimeInMillis(selectedDueDateMillis);
        }

        DatePickerDialog pickerDialog = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(year, month, dayOfMonth, 12, 0, 0);
                    chosen.set(Calendar.MILLISECOND, 0);
                    selectedDueDateMillis = chosen.getTimeInMillis();
                    updateDueHintText(selectedDueDateMillis);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        pickerDialog.setOnCancelListener(dialog -> {
            chipWeek.setChecked(true);
            applyPresetDueDays(7);
        });

        pickerDialog.show();
    }

    private void updateDueHintText(long dueMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", new Locale("ar"));
        String formattedDate = sdf.format(new Date(dueMillis));
        tvDueHint.setText(getString(R.string.customer_due_hint, formattedDate));
    }

    private void validateAndSaveCustomer() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), getString(R.string.login_required), Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String amountStr = etAmount.getText() != null ? etAmount.getText().toString().trim() : "";

        if (name.isEmpty()) {
            etName.setError(getString(R.string.customer_name_required));
            etName.requestFocus();
            return;
        }

        for (String existing : existingCustomerNames) {
            if (existing.equalsIgnoreCase(name)) {
                Toast.makeText(requireContext(), getString(R.string.customer_exists), Toast.LENGTH_LONG).show();
                return;
            }
        }

        double initialAmount = 0.0;
        if (!amountStr.isEmpty()) {
            Double parsed = parseDoubleSafe(amountStr);
            if (parsed == null || parsed < 0) {
                Toast.makeText(requireContext(), getString(R.string.error_amount_invalid), Toast.LENGTH_SHORT).show();
                return;
            }
            initialAmount = Math.round(parsed * 100.0) / 100.0;
        }

        btnSave.setEnabled(false);

        String customerId = db.collection("customers").document().getId();
        long now = System.currentTimeMillis();

        Map<String, Object> customerMap = new HashMap<>();
        customerMap.put("name", name);
        customerMap.put("phone", phone);
        customerMap.put("totalDebt", initialAmount);
        customerMap.put("dueDate", selectedDueDateMillis);
        customerMap.put("userId", userId);
        customerMap.put("archived", false);
        customerMap.put("deletedAt", 0L);

        WriteBatch batch = db.batch();
        DocumentReference customerRef = db.collection("customers").document(customerId);
        batch.set(customerRef, customerMap);

        if (initialAmount > 0) {
            String debtId = customerRef.collection("debts").document().getId();
            Map<String, Object> debtMap = new HashMap<>();
            debtMap.put("id", debtId);
            debtMap.put("customerId", customerId);
            debtMap.put("itemName", getString(R.string.customer_first_debt_note));
            debtMap.put("amount", initialAmount);
            debtMap.put("timestamp", now);
            debtMap.put("dueDate", selectedDueDateMillis);
            debtMap.put("archived", false);

            DocumentReference debtRef = customerRef.collection("debts").document(debtId);
            batch.set(debtRef, debtMap);
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), getString(R.string.customer_saved), Toast.LENGTH_SHORT).show();
                        dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        btnSave.setEnabled(true);
                        Toast.makeText(requireContext(), getString(R.string.saved_offline), Toast.LENGTH_SHORT).show();
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