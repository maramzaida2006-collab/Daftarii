package com.example.daftarii;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ArchivedCustomersAdapter extends RecyclerView.Adapter<ArchivedCustomersAdapter.ViewHolder> {

    public interface OnCustomerActionListener {
        void onRestoreClick(Customer customer);
        void onDeleteClick(Customer customer);
    }

    private List<Customer> customers = new ArrayList<>();
    private final OnCustomerActionListener listener;

    public ArchivedCustomersAdapter(OnCustomerActionListener listener) {
        this.listener = listener;
    }

    public void setCustomers(List<Customer> list) {
        this.customers = (list != null) ? list : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_archived_customer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Customer customer = customers.get(position);

        String name = (customer.getName() != null && !customer.getName().trim().isEmpty())
                ? customer.getName().trim()
                : "زبون";

        holder.tvName.setText(name);
        holder.tvAvatar.setText(name.substring(0, 1));

        String phone = (customer.getPhone() != null && !customer.getPhone().trim().isEmpty())
                ? customer.getPhone().trim()
                : "بدون رقم";
        holder.tvPhone.setText(phone);

        String formattedDebt = String.format(Locale.US, "%.2f", Math.max(0.0, customer.getTotalDebt()));
        holder.tvDebt.setText("₪ " + formattedDebt);

        holder.btnRestore.setOnClickListener(v -> {
            if (listener != null) listener.onRestoreClick(customer);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(customer);
        });
    }

    @Override
    public int getItemCount() {
        return customers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvName, tvPhone, tvDebt;
        LinearLayout btnRestore;
        FrameLayout btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            tvName = itemView.findViewById(R.id.tvName);
            tvPhone = itemView.findViewById(R.id.tvPhone);
            tvDebt = itemView.findViewById(R.id.tvDebt);
            btnRestore = itemView.findViewById(R.id.btnRestore);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}