package com.example.daftarii.logic;

import static com.example.daftarii.logic.Validators.Result.EMPTY;
import static com.example.daftarii.logic.Validators.Result.INVALID_FORMAT;
import static com.example.daftarii.logic.Validators.Result.NOT_POSITIVE;
import static com.example.daftarii.logic.Validators.Result.OK;
import static com.example.daftarii.logic.Validators.Result.TOO_LARGE;
import static com.example.daftarii.logic.Validators.Result.TOO_LONG;
import static com.example.daftarii.logic.Validators.Result.TOO_SHORT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ValidatorsTest {

    // ---------- الإيميل ----------

    @Test
    public void email_anyValidDomain_notOnlyGmail() {
        assertEquals(OK, Validators.email("sondos@gmail.com"));
        assertEquals(OK, Validators.email("sskhail@smail.ucas.edu.ps"));
        assertEquals(OK, Validators.email("  shop@outlook.com "));
    }

    @Test
    public void email_invalid() {
        assertEquals(EMPTY, Validators.email(""));
        assertEquals(EMPTY, Validators.email(null));
        assertEquals(INVALID_FORMAT, Validators.email("sondos"));
        assertEquals(INVALID_FORMAT, Validators.email("sondos@"));
        assertEquals(INVALID_FORMAT, Validators.email("sondos@mail"));
    }

    // ---------- كلمة السر ----------

    @Test
    public void password_minSix() {
        assertEquals(OK, Validators.password("123456"));
        assertEquals(TOO_SHORT, Validators.password("12345"));
        assertEquals(EMPTY, Validators.password(""));
    }

    @Test
    public void password_spacesCount_noTrim() {
        // "  1234" فيها 6 أحرف مع المسافات
        assertEquals(OK, Validators.password("  1234"));
    }

    // ---------- PIN ----------

    @Test
    public void pin_fourDigits() {
        assertEquals(OK, Validators.pin("1234"));
        assertEquals(OK, Validators.pin("١٢٣٤"));
        assertEquals(INVALID_FORMAT, Validators.pin("123"));
        assertEquals(INVALID_FORMAT, Validators.pin("12345"));
        assertEquals(INVALID_FORMAT, Validators.pin("12a4"));
        assertEquals(EMPTY, Validators.pin(""));
    }

    // ---------- اسم الزبون ----------

    @Test
    public void customerName_required() {
        assertEquals(OK, Validators.customerName("أبو أحمد"));
        assertEquals(EMPTY, Validators.customerName("   "));
        assertEquals(EMPTY, Validators.customerName(null));
    }

    @Test
    public void customerName_maxLength() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Validators.NAME_MAX; i++) sb.append('ا');
        assertEquals(OK, Validators.customerName(sb.toString()));
        assertEquals(TOO_LONG, Validators.customerName(sb.append('ا').toString()));
    }

    // ---------- رقم الجوال ----------

    @Test
    public void phone_optional() {
        assertEquals(OK, Validators.phone(""));
        assertEquals(OK, Validators.phone(null));
    }

    @Test
    public void phone_validFormats() {
        assertEquals(OK, Validators.phone("0591234567"));
        assertEquals(OK, Validators.phone("059-123 4567"));
        assertEquals(OK, Validators.phone("+970591234567"));
        assertEquals(OK, Validators.phone("٠٥٩١٢٣٤٥٦٧"));
        assertEquals(OK, Validators.phone("082345678")); // أرضي
    }

    @Test
    public void phone_invalid() {
        assertEquals(INVALID_FORMAT, Validators.phone("12345"));
        assertEquals(INVALID_FORMAT, Validators.phone("05912345678901"));
        assertEquals(INVALID_FORMAT, Validators.phone("05abc34567"));
    }

    @Test
    public void normalizePhone_sameNumberDifferentForms() {
        String expected = "0591234567";
        assertEquals(expected, Validators.normalizePhone("0591234567"));
        assertEquals(expected, Validators.normalizePhone("+970 59 123 4567"));
        assertEquals(expected, Validators.normalizePhone("00970-59-123-4567"));
        assertEquals(expected, Validators.normalizePhone("+972591234567"));
        assertEquals(expected, Validators.normalizePhone("٠٥٩١٢٣٤٥٦٧"));
    }

    // ---------- المبلغ ----------

    @Test
    public void amount_valid() {
        assertEquals(OK, Validators.amount("45.50"));
        assertEquals(OK, Validators.amount("0.01"));
        assertEquals(OK, Validators.amount("٢٠"));
    }

    @Test
    public void amount_mustBePositive() {
        assertEquals(NOT_POSITIVE, Validators.amount("0"));
        assertEquals(NOT_POSITIVE, Validators.amount("0.00"));
    }

    @Test
    public void amount_invalid_doesNotCrash() {
        // بالكود القديم هدول كانوا بيعملوا كراش بـ parseDouble
        assertEquals(EMPTY, Validators.amount(""));
        assertEquals(EMPTY, Validators.amount(null));
        assertEquals(INVALID_FORMAT, Validators.amount("abc"));
        assertEquals(INVALID_FORMAT, Validators.amount("-5"));
        assertEquals(INVALID_FORMAT, Validators.amount("1.234"));
        assertEquals(INVALID_FORMAT, Validators.amount("."));
    }

    @Test
    public void amount_tooLarge() {
        assertEquals(TOO_LARGE, Validators.amount("1000000.01"));
        assertEquals(TOO_LARGE, Validators.amount("99999999999"));
        assertEquals(OK, Validators.amount("1000000"));
    }

    // ---------- الوصف ----------

    @Test
    public void note_optionalWithMax() {
        assertEquals(OK, Validators.note(null));
        assertEquals(OK, Validators.note(""));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i <= Validators.NOTE_MAX; i++) sb.append('x');
        assertEquals(TOO_LONG, Validators.note(sb.toString()));
    }
}
