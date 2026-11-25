package com.example.daftarii;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import com.example.daftarii.auth.AuthMessages;
import com.example.daftarii.logic.Money;
import com.example.daftarii.logic.TransactionEdit;
import com.example.daftarii.logic.TransactionType;
import com.example.daftarii.logic.Validators;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

/**
 * تعديل حركة (Bottom Sheet) حسب تصميم EditTransaction:
 * المبلغ والوصف بس (النوع ما بيتغيّر)، وأثر التعديل على الرصيد قبل الحفظ،
 * وسجل التعديلات، وزر أرشفة (بيرجع لشاشة كشف الزبون عشان التأكيد).
 *
 * الشاشة اللي بتفتحه لازم تطبّق {@link Host}.
 */
public class EditTransactionSheet extends BottomSheetDialogFragment {

    /** كشف الزبون بيطبّقها عشان يحدّث القائمة والرصيد، ويأرشف. */
    public interface Host {
        void onTransactionEdited(String debtId, String newNote, double newSignedAmount, double balanceDiff);
        void onArchiveRequested(String debtId);
    }

    private static final String ARG_CUSTOMER_ID = "customerId";
    private static final String ARG_CUSTOMER_NAME = "customerName";
    private static final String ARG_DEBT_ID = "debtId";
    private static final String ARG_NOTE = "note";
    private static final String ARG_AMOUNT = "amount";
    private static final String ARG_TIME = "time";

    private String customerId, debtId, oldNote;
    private TransactionType type;
    private long oldAgorot;
    private long createdAt;

    private EditText etAmount;
    private TextInputEditText etNote;
    private TextInputLayout tilNote;
    private TextView tvDiff;
    private LinearLayout historyContainer;
    private View btnSave;

    public static EditTransactionSheet newInstance(String customerId, String customerName, DebtItem item) {
        Bundle b = new Bundle();
        b.putString(ARG_CUSTOMER_ID, customerId);
        b.putString(ARG_CUSTOMER_NAME, customerName);
        b.putString(ARG_DEBT_ID, item.getId());
        b.putString(ARG_NOTE, item.getItemName());
        b.putDouble(ARG_AMOUNT, item.getAmount());
        b.putLong(ARG_TIME, item.getTimestamp());
        EditTransactionSheet sheet = new EditTransactionSheet();
        sheet.setArguments(b);
        return sheet;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        // الكيبورد ما بيغطي زر "حفظ التعديل"
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        dialog.setOnShowListener(d -> {
            FrameLayout sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                sheet.setBackgroundResource(android.R.color.transparent);
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.edit_transaction_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = requireArguments();
        customerId = args.getString(ARG_CUSTOMER_ID);
        debtId = args.getString(ARG_DEBT_ID);
        oldNote = args.getString(ARG_NOTE, "");
        double signed = args.getDouble(ARG_AMOUNT);
        // بالبنية الحالية الدفعة مبلغ سالب
        type = signed < 0 ? TransactionType.PAYMENT : TransactionType.DEBT;
        oldAgorot = Math.round(Math.abs(signed) * 100);
        createdAt = args.getLong(ARG_TIME);

        Context c = requireContext();
        etAmount = view.findViewById(R.id.etEditAmount);
        etNote = view.findViewById(R.id.etEditNote);
        tilNote = view.findViewById(R.id.tilEditNote);
        tvDiff = view.findViewById(R.id.tvEditDiff);
        historyContainer = view.findViewById(R.id.historyContainer);
        btnSave = view.findViewById(R.id.btnEditSave);

        // العنوان الفرعي: اسم الزبون · وقت الحركة
        String name = args.getString(ARG_CUSTOMER_NAME, "");
        String when = createdAt > 0 ? DateUtils.formatDateTime(c, createdAt,
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_ABBREV_MONTH) : "";
        ((TextView) view.findViewById(R.id.tvEditSubtitle)).setText(
                name.isEmpty() ? when : (when.isEmpty() ? name : name + " · " + when));

        // شارة النوع (ما بتتغيّر)
        TextView tvType = view.findViewById(R.id.tvEditType);
        boolean debt = type == TransactionType.DEBT;
        tvType.setText(debt ? R.string.edit_tx_type_debt : R.string.edit_tx_type_payment);
        tvType.setTextColor(ContextCompat.getColor(c, debt ? R.color.debt_fg : R.color.payment_fg));
        ViewCompat.setBackgroundTintList(tvType, ColorStateList.valueOf(
                ContextCompat.getColor(c, debt ? R.color.debt_bg : R.color.payment_bg)));

        etAmount.setText(Money.format(oldAgorot));
        etNote.setText(oldNote);
        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable s) { updateDiff(); }
        });
        updateDiff();

        view.findViewById(R.id.btnEditArchive).setOnClickListener(v -> {
            if (getActivity() instanceof Host) ((Host) getActivity()).onArchiveRequested(debtId);
            dismiss();
        });
        btnSave.setOnClickListener(v -> save());

        showHistory(null);
        if (customerId != null && debtId != null) {
            TransactionEditor.loadHistory(customerId, debtId, history -> {
                if (isAdded()) showHistory(history);
            });
        }
    }

    /** "الرصيد رح يتعدّل بالفرق: + ₪ 5.50"، أو خطأ إذا المبلغ مش صح. */
    private void updateDiff() {
        Context c = requireContext();
        String input = etAmount.getText() != null ? etAmount.getText().toString() : "";
        Validators.Result r = Validators.amount(input);
        if (r != Validators.Result.OK) {
            Integer err = AuthMessages.amountError(r);
            tvDiff.setText(err != null ? err : R.string.edit_tx_amount_invalid);
            tvDiff.setTextColor(ContextCompat.getColor(c, R.color.error));
            btnSave.setEnabled(false);
            return;
        }
        btnSave.setEnabled(true);
        long diff = TransactionEdit.balanceDiff(type, oldAgorot, Money.parseToAgorot(input));
        if (diff == 0) {
            tvDiff.setText(R.string.edit_tx_no_balance_change);
            tvDiff.setTextColor(ContextCompat.getColor(c, R.color.text_secondary));
        } else {
            String amount = (diff > 0 ? "+ ₪ " : "− ₪ ") + Money.format(Math.abs(diff));
            tvDiff.setText(getString(R.string.edit_tx_balance_diff, amount));
            tvDiff.setTextColor(ContextCompat.getColor(c, diff > 0 ? R.color.debt_fg : R.color.payment_fg));
        }
    }

    private void save() {
        String input = etAmount.getText() != null ? etAmount.getText().toString() : "";
        if (Validators.amount(input) != Validators.Result.OK) return;
        String note = etNote.getText() != null ? etNote.getText().toString().trim() : "";
        if (Validators.note(note) != Validators.Result.OK) {
            tilNote.setError(getString(R.string.edit_tx_note_too_long));
            return;
        }
        tilNote.setError(null);

        long newAgorot = Money.parseToAgorot(input);
        TransactionEdit.Entry entry = TransactionEdit.entry(oldAgorot, newAgorot, oldNote, note,
                System.currentTimeMillis());
        if (entry == null) {
            dismiss(); // ما تغيّر إشي
            return;
        }
        if (customerId == null || debtId == null) return;

        double newSigned = (type == TransactionType.PAYMENT ? -newAgorot : newAgorot) / 100.0;
        double diff = TransactionEdit.balanceDiff(type, oldAgorot, newAgorot) / 100.0;
        btnSave.setEnabled(false);

        // Firestore بيحفظ محلياً فوراً وبيكمّل لما يرجع النت، فبنحدّث الشاشة مباشرة
        TransactionEditor.save(customerId, debtId, note, newSigned, diff, entry, ok -> {
            Context c = getContext();
            if (c != null) {
                Toast.makeText(c, ok ? R.string.edit_tx_saved : R.string.edit_tx_saved_offline,
                        Toast.LENGTH_SHORT).show();
            }
        });
        if (getActivity() instanceof Host) {
            ((Host) getActivity()).onTransactionEdited(debtId, note, newSigned, diff);
        }
        dismiss();
    }

    /** السجل: كل تعديل (الأحدث فوق)، وآخر سطر "إنشاء الحركة بمبلغ ...". */
    private void showHistory(@Nullable List<TransactionEdit.Entry> history) {
        Context c = requireContext();
        historyContainer.removeAllViews();
        LayoutInflater inf = LayoutInflater.from(c);
        List<TransactionEdit.Entry> rows = TransactionEdit.newestFirst(history);
        for (TransactionEdit.Entry e : rows) {
            addHistoryRow(inf, describe(c, e), e.at, true, false);
        }
        long original = TransactionEdit.originalAmount(history, oldAgorot);
        addHistoryRow(inf, getString(R.string.edit_tx_history_created, Money.format(original)),
                createdAt, rows.isEmpty(), true);
    }

    private String describe(Context c, TransactionEdit.Entry e) {
        if (e.amountChanged() && e.noteChanged()) {
            return c.getString(R.string.edit_tx_history_both,
                    Money.format(e.oldAmount), Money.format(e.newAmount));
        }
        if (e.amountChanged()) {
            return c.getString(R.string.edit_tx_history_amount,
                    Money.format(e.oldAmount), Money.format(e.newAmount));
        }
        return c.getString(R.string.edit_tx_history_note, e.oldNote, e.newNote);
    }

    private void addHistoryRow(LayoutInflater inf, String title, long time, boolean latest, boolean last) {
        View row = inf.inflate(R.layout.item_edit_history, historyContainer, false);
        ((TextView) row.findViewById(R.id.tvHistoryTitle)).setText(title);
        ((TextView) row.findViewById(R.id.tvHistoryTime)).setText(time > 0
                ? DateUtils.getRelativeDateTimeString(requireContext(), time,
                DateUtils.MINUTE_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0) : "");
        ViewCompat.setBackgroundTintList(row.findViewById(R.id.historyDot), ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), latest ? R.color.violet : R.color.dot_inactive)));
        row.findViewById(R.id.historyLine).setVisibility(last ? View.INVISIBLE : View.VISIBLE);
        historyContainer.addView(row);
    }
}
