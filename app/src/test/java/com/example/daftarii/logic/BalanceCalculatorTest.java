package com.example.daftarii.logic;

import static com.example.daftarii.logic.TransactionType.DEBT;
import static com.example.daftarii.logic.TransactionType.PAYMENT;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class BalanceCalculatorTest {

    @Test
    public void effect_debtIncreasesBalance() {
        assertEquals(5000, BalanceCalculator.effect(DEBT, 5000));
    }

    @Test
    public void effect_paymentDecreasesBalance() {
        assertEquals(-2000, BalanceCalculator.effect(PAYMENT, 2000));
    }

    @Test(expected = IllegalArgumentException.class)
    public void effect_negativeAmount_throws() {
        BalanceCalculator.effect(DEBT, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void effect_nullType_throws() {
        BalanceCalculator.effect(null, 100);
    }

    @Test
    public void archive_reversesEffect() {
        assertEquals(-5000, BalanceCalculator.archiveEffect(DEBT, 5000));
        assertEquals(2000, BalanceCalculator.archiveEffect(PAYMENT, 2000));
    }

    @Test
    public void restore_reappliesEffect() {
        assertEquals(5000, BalanceCalculator.restoreEffect(DEBT, 5000));
        assertEquals(-2000, BalanceCalculator.restoreEffect(PAYMENT, 2000));
    }

    @Test
    public void archiveThenRestore_returnsToSameBalance() {
        long balance = 10000;
        balance += BalanceCalculator.archiveEffect(PAYMENT, 3000);
        balance += BalanceCalculator.restoreEffect(PAYMENT, 3000);
        assertEquals(10000, balance);
    }

    @Test
    public void editDiff_debtIncreased() {
        // دين 50 صار 80: الرصيد بيزيد 30
        assertEquals(3000, BalanceCalculator.editDiff(DEBT, 5000, 8000));
    }

    @Test
    public void editDiff_debtDecreased() {
        assertEquals(-2000, BalanceCalculator.editDiff(DEBT, 5000, 3000));
    }

    @Test
    public void editDiff_paymentIncreased() {
        // دفعة 20 صارت 50: الرصيد بينقص 30
        assertEquals(-3000, BalanceCalculator.editDiff(PAYMENT, 2000, 5000));
    }

    @Test
    public void editDiff_sameAmount_isZero() {
        assertEquals(0, BalanceCalculator.editDiff(PAYMENT, 2000, 2000));
    }

    @Test
    public void balanceAfter_showsPreviewBeforeSaving() {
        assertEquals(7000, BalanceCalculator.balanceAfter(10000, PAYMENT, 3000));
        assertEquals(13000, BalanceCalculator.balanceAfter(10000, DEBT, 3000));
    }

    @Test
    public void isOverpayment() {
        assertTrue(BalanceCalculator.isOverpayment(1000, PAYMENT, 1500));
        assertFalse(BalanceCalculator.isOverpayment(1000, PAYMENT, 1000));
        assertFalse(BalanceCalculator.isOverpayment(1000, DEBT, 5000));
    }

    @Test
    public void runningBalances_forStatement() {
        long[] result = BalanceCalculator.runningBalances(
                Arrays.asList(DEBT, DEBT, PAYMENT, DEBT),
                Arrays.asList(5000L, 2550L, 3000L, 1000L));
        assertArrayEquals(new long[]{5000, 7550, 4550, 5550}, result);
    }

    @Test
    public void runningBalances_empty() {
        assertArrayEquals(new long[0], BalanceCalculator.runningBalances(
                Collections.<TransactionType>emptyList(), Collections.<Long>emptyList()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void runningBalances_sizeMismatch_throws() {
        BalanceCalculator.runningBalances(Arrays.asList(DEBT), Arrays.asList(1L, 2L));
    }

    @Test
    public void transactionType_fromString() {
        assertEquals(DEBT, TransactionType.fromString("DEBT"));
        assertEquals(PAYMENT, TransactionType.fromString("PAYMENT"));
        assertEquals(null, TransactionType.fromString("debt"));
        assertEquals(null, TransactionType.fromString(null));
    }
}
