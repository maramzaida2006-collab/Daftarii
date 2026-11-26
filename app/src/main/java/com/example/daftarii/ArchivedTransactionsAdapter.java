package com.example.daftarii;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ArchivedTransactionsAdapter extends RecyclerView.Adapter<ArchivedTransactionsAdapter.ViewHolder> {

    public interface OnTransactionActionListener {
        void onRestoreClick(Transaction transaction);
        void onDeleteClick(Transaction transaction);
    }

    private List<Transaction> transactions = new ArrayList<>();
    private final OnTransactionActionListener listener;

    public ArchivedTransactionsAdapter(OnTransactionActionListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<Transaction> list) {
        this.transactions = (list != null) ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_archived, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction item = transactions.get(position);

        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : holder.itemView.getContext().getString(R.string.tx_untitled));

        String dateText = formatArchivedDate(holder.itemView.getContext(), item.getDeletedAt());
        String customerName = item.getCustomerName() != null ? item.getCustomerName() : "";
        holder.tvSubtitle.setText(
                customerName.isEmpty() ? dateText : customerName + " · " + dateText
        );

        boolean isPayment = "PAYMENT".equalsIgnoreCase(item.getType());
        String formattedAmount = String.format(Locale.US, "%.2f", Math.abs(item.getAmount()));

        if (isPayment) {
            holder.tvAmount.setText("- ₪ " + formattedAmount);
            holder.tvAmount.setTextColor(Color.parseColor("#1E8449"));
            holder.ivDirection.setColorFilter(Color.parseColor("#1E8449"));
            holder.iconBox.setBackgroundResource(R.drawable.bg_icon_green);
        } else {
            holder.tvAmount.setText("+ ₪ " + formattedAmount);
            holder.tvAmount.setTextColor(Color.parseColor("#C0392B"));
            holder.ivDirection.setColorFilter(Color.parseColor("#C0392B"));
            holder.iconBox.setBackgroundResource(R.drawable.bg_icon_red);
        }

        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) listener.onRestoreClick(item);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    private String formatArchivedDate(android.content.Context c, long deletedAt) {
        if (deletedAt <= 0) return c.getString(R.string.archived_earlier);

        Calendar now = Calendar.getInstance();
        Calendar archivedCal = Calendar.getInstance();
        archivedCal.setTimeInMillis(deletedAt);

        boolean sameYear = now.get(Calendar.YEAR) == archivedCal.get(Calendar.YEAR);
        boolean sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == archivedCal.get(Calendar.DAY_OF_YEAR);

        now.add(Calendar.DAY_OF_YEAR, -1);
        boolean yesterday = sameYear && now.get(Calendar.DAY_OF_YEAR) == archivedCal.get(Calendar.DAY_OF_YEAR);

        if (sameDay) {
            return c.getString(R.string.archived_today);
        } else if (yesterday) {
            return c.getString(R.string.archived_yesterday);
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("d MMMM", Locale.getDefault());
            return c.getString(R.string.archived_on, sdf.format(new Date(deletedAt)));
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        FrameLayout iconBox;
        ImageView ivDirection;
        TextView tvTitle, tvSubtitle, tvAmount;
        LinearLayout btnRestore;
        FrameLayout btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            iconBox = itemView.findViewById(R.id.iconBox);
            ivDirection = itemView.findViewById(R.id.ivDirection);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvAmount = itemView.findViewById(R.id.tvAmount);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}