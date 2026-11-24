package com.example.daftarii.logic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class MoneyTest {

    @Test
    public void parse_wholeNumber() {
        assertEquals(Long.valueOf(4500), Money.parseToAgorot("45"));
    }

    @Test
    public void parse_twoDecimals() {
        assertEquals(Long.valueOf(4550), Money.parseToAgorot("45.50"));
    }

    @Test
    public void parse_oneDecimal_isTens() {
        assertEquals(Long.valueOf(4550), Money.parseToAgorot("45.5"));
    }

    @Test
    public void parse_leadingDot() {
        assertEquals(Long.valueOf(50), Money.parseToAgorot(".5"));
    }

    @Test
    public void parse_arabicDigitsAndSeparator() {
        assertEquals(Long.valueOf(4550), Money.parseToAgorot("٤٥٫٥٠"));
    }

    @Test
    public void parse_commaSeparator() {
        assertEquals(Long.valueOf(1025), Money.parseToAgorot("10,25"));
    }

    @Test
    public void parse_trimsSpaces() {
        assertEquals(Long.valueOf(700), Money.parseToAgorot("  7 "));
    }

    @Test
    public void parse_noFloatingPointError() {
        // 0.1 + 0.2 بـ double بتطلع 0.30000000000000004
        long sum = Money.parseToAgorot("0.1") + Money.parseToAgorot("0.2");
        assertEquals(30, sum);
    }

    @Test
    public void parse_invalidInputs_returnNull() {
        assertNull(Money.parseToAgorot(null));
        assertNull(Money.parseToAgorot(""));
        assertNull(Money.parseToAgorot("abc"));
        assertNull(Money.parseToAgorot("12.345"));
        assertNull(Money.parseToAgorot("-5"));
        assertNull(Money.parseToAgorot("1.2.3"));
        assertNull(Money.parseToAgorot("."));
    }

    @Test
    public void parse_aboveMax_returnsNull() {
        assertNull(Money.parseToAgorot("1000000.01"));
        assertEquals(Long.valueOf(Money.MAX_AGOROT), Money.parseToAgorot("1000000"));
    }

    @Test
    public void format_values() {
        assertEquals("45.50", Money.format(4550));
        assertEquals("0.05", Money.format(5));
        assertEquals("0.00", Money.format(0));
        assertEquals("-3.00", Money.format(-300));
        assertEquals("1000000.00", Money.format(100_000_000L));
    }
}
