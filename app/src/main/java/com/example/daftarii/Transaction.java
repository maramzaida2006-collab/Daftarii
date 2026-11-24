package com.example.daftarii;

public class Transaction {
    private String id;
    private String customerId;
    private String customerName;
    private String title;
    private double amount;
    private String type; // "DEBT" (دين/إضافة) أو "PAYMENT" (تسديد/دفعة)
    private long timestamp;
    private boolean archived = false;
    private long deletedAt = 0;

    public Transaction() {}

    public Transaction(String id, String customerId, String customerName, String title,
                       double amount, String type, long timestamp) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.title = title;
        this.amount = amount;
        this.type = type;
        this.timestamp = timestamp;
        this.archived = false;
        this.deletedAt = 0;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    public long getDeletedAt() { return deletedAt; }
    public void setDeletedAt(long deletedAt) { this.deletedAt = deletedAt; }
}