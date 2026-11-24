package com.example.daftarii.logic;

import java.util.List;

/**
 * المكان الوحيد اللي بيقرر قديش بيتغيّر رصيد الزبون.
 * كل الأرقام بالأغورة. القيمة اللي بترجع بتنبعت لـ FieldValue.increment()
 * بنفس الـ batch مع الحركة.
 *
 * | العملية  | التغيير على الرصيد        |
 * |----------|---------------------------|
 * | دين      | +amount                   |
 * | دفعة     | -amount                   |
 * | أرشفة    | عكس أثر الحركة            |
 * | استرجاع  | أثر الحركة نفسه           |
 * | تعديل    | الفرق بين الجديد والقديم  |
 */
public final class BalanceCalculator {

    private BalanceCalculator() {}

    /** أثر حركة جديدة على الرصيد. */
    public static long effect(TransactionType type, long amount) {
        if (type == null) throw new IllegalArgumentException("type is null");
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
        return type == TransactionType.DEBT ? amount : -amount;
    }

    /** أثر أرشفة حركة: بيلغي أثرها. */
    public static long archiveEffect(TransactionType type, long amount) {
        return -effect(type, amount);
    }

    /** أثر استرجاع حركة من الأرشيف: بيرجّع أثرها. */
    public static long restoreEffect(TransactionType type, long amount) {
        return effect(type, amount);
    }

    /**
     * أثر تعديل مبلغ حركة. النوع ما بيتغيّر بالتعديل.
     * مثال: دين 50 صار 80، الرصيد بيزيد 30.
     */
    public static long editDiff(TransactionType type, long oldAmount, long newAmount) {
        return effect(type, newAmount) - effect(type, oldAmount);
    }

    /** الرصيد بعد الحركة. بيظهر للمستخدم قبل الحفظ. */
    public static long balanceAfter(long currentBalance, TransactionType type, long amount) {
        return currentBalance + effect(type, amount);
    }

    /**
     * هل الدفعة أكبر من الرصيد؟ القرار شو نعمل (نمنع ولا نحذّر) لسا عند الـ PO،
     * فالواجهة بتسأل هالدالة وبتقرر.
     */
    public static boolean isOverpayment(long currentBalance, TransactionType type, long amount) {
        return type == TransactionType.PAYMENT && amount > currentBalance;
    }

    /**
     * الرصيد بعد كل حركة، للـ PDF وكشف الزبون.
     * لازم الحركات تكون مرتبة من الأقدم للأحدث، وبدون المؤرشفة.
     *
     * @return مصفوفة بنفس طول القائمة: العنصر i هو الرصيد بعد الحركة i.
     */
    public static long[] runningBalances(List<TransactionType> types, List<Long> amounts) {
        if (types.size() != amounts.size()) {
            throw new IllegalArgumentException("types and amounts must have the same size");
        }
        long[] result = new long[types.size()];
        long balance = 0;
        for (int i = 0; i < types.size(); i++) {
            balance += effect(types.get(i), amounts.get(i));
            result[i] = balance;
        }
        return result;
    }
}
