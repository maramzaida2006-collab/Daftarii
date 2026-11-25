package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TransactionEditTest {

    @Test
    public void entry_nothingChanged_isNull() {
        assertNull(TransactionEdit.entry(3000, 3000, "مواد تنظيف", " مواد تنظيف ", 1));
        assertNull(TransactionEdit.entry(3000, 3000, null, "", 1));
    }

    @Test
    public void entry_amountChanged() {
        TransactionEdit.Entry e = TransactionEdit.entry(3000, 3550, "مواد", "مواد", 99);
        assertNotNull(e);
        assertTrue(e.amountChanged());
        assertFalse(e.noteChanged());
        assertEquals(99, e.at);
    }

    @Test
    public void entry_noteChangedOnly() {
        TransactionEdit.Entry e = TransactionEdit.entry(3000, 3000, "مواد", "مواد تنظيف", 1);
        assertNotNull(e);
        assertFalse(e.amountChanged());
        assertTrue(e.noteChanged());
    }

    @Test
    public void balanceDiff_debtAndPayment() {
        // دين 30 صار 35.50: الرصيد بيزيد 5.50
        assertEquals(550, TransactionEdit.balanceDiff(TransactionType.DEBT, 3000, 3550));
        // دفعة 200 صارت 150: الرصيد بيزيد 50 (انخصم أقل)
        assertEquals(5000, TransactionEdit.balanceDiff(TransactionType.PAYMENT, 20000, 15000));
        // دفعة 100 صارت 120: الرصيد بيقل 20
        assertEquals(-2000, TransactionEdit.balanceDiff(TransactionType.PAYMENT, 10000, 12000));
        assertEquals(0, TransactionEdit.balanceDiff(TransactionType.DEBT, 500, 500));
    }

    @Test
    public void originalAmount_fromOldestEntry() {
        List<TransactionEdit.Entry> history = Arrays.asList(
                new TransactionEdit.Entry(200, 3550, 4000, "", ""),
                new TransactionEdit.Entry(100, 3000, 3550, "", ""));
        assertEquals(3000, TransactionEdit.originalAmount(history, 4000));
        assertEquals(4000, TransactionEdit.originalAmount(Collections.emptyList(), 4000));
        assertEquals(4000, TransactionEdit.originalAmount(null, 4000));
    }

    @Test
    public void newestFirst_sorts() {
        List<TransactionEdit.Entry> sorted = TransactionEdit.newestFirst(Arrays.asList(
                new TransactionEdit.Entry(100, 1, 2, "", ""),
                new TransactionEdit.Entry(300, 2, 3, "", ""),
                new TransactionEdit.Entry(200, 3, 4, "", "")));
        assertEquals(300, sorted.get(0).at);
        assertEquals(100, sorted.get(2).at);
        assertTrue(TransactionEdit.newestFirst(null).isEmpty());
    }
}
