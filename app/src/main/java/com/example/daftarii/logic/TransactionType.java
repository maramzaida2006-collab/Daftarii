package com.example.daftarii.logic;

/**
 * نوع الحركة. بينحفظ بـ Firestore كنص: "DEBT" أو "PAYMENT".
 * المبلغ دايماً موجب، والنوع هو اللي بيحدد الإشارة.
 */
public enum TransactionType {
    DEBT,
    PAYMENT;

    /** بيحوّل النص من Firestore لنوع. بيرجّع null إذا النص مش معروف. */
    public static TransactionType fromString(String value) {
        if (value == null) return null;
        try {
            return TransactionType.valueOf(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
