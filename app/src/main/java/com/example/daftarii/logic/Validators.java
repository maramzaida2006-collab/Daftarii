package com.example.daftarii.logic;

import java.util.regex.Pattern;

/**
 * التحقق من المدخلات. كل دالة بترجّع Result، والواجهة بتحوّله لرسالة من strings.xml.
 * ما في نصوص للمستخدم هون، عشان الـ Definition of Done.
 */
public final class Validators {

    public enum Result {
        OK,
        EMPTY,
        TOO_LONG,
        INVALID_FORMAT,
        TOO_SHORT,
        NOT_POSITIVE,
        TOO_LARGE,
        MISMATCH
    }

    public static final int NAME_MAX = 50;
    public static final int NOTE_MAX = 200;
    public static final int PASSWORD_MIN = 6;
    public static final int PIN_LENGTH = 4;

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private Validators() {}

    /** أي إيميل صحيح، مش Gmail بس. */
    public static Result email(String email) {
        if (email == null || email.trim().isEmpty()) return Result.EMPTY;
        return EMAIL.matcher(email.trim()).matches() ? Result.OK : Result.INVALID_FORMAT;
    }

    /** 6 أحرف أو أكتر. ما بنعمل trim لكلمة السر: المسافة جزء منها. */
    public static Result password(String password) {
        if (password == null || password.isEmpty()) return Result.EMPTY;
        return password.length() >= PASSWORD_MIN ? Result.OK : Result.TOO_SHORT;
    }

    /** تأكيد كلمة السر: لازم يكون نفسها حرف بحرف. */
    public static Result passwordConfirmation(String password, String confirmation) {
        if (confirmation == null || confirmation.isEmpty()) return Result.EMPTY;
        return confirmation.equals(password) ? Result.OK : Result.MISMATCH;
    }

    /** اسم المحل بشاشة إنشاء حساب: إجباري ولحد 50 حرف، متل اسم الزبون. */
    public static Result shopName(String name) {
        return customerName(name);
    }

    /** الـ PIN: 4 أرقام بالضبط. */
    public static Result pin(String pin) {
        if (pin == null || pin.isEmpty()) return Result.EMPTY;
        String p = Money.normalizeDigits(pin);
        if (!p.matches("\\d+")) return Result.INVALID_FORMAT;
        return p.length() == PIN_LENGTH ? Result.OK : Result.INVALID_FORMAT;
    }

    /** اسم الزبون إجباري. */
    public static Result customerName(String name) {
        if (name == null || name.trim().isEmpty()) return Result.EMPTY;
        return name.trim().length() <= NAME_MAX ? Result.OK : Result.TOO_LONG;
    }

    /** رقم الجوال اختياري. إذا انكتب لازم يكون 9–10 أرقام بعد التنظيف. */
    public static Result phone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return Result.OK;
        String p = normalizePhone(phone);
        return p.matches("0\\d{8,9}") ? Result.OK : Result.INVALID_FORMAT;
    }

    /**
     * بيوحّد شكل الرقم عشان نمنع التكرار: بيشيل المسافات والشرطات،
     * وبيحوّل +970 / 00970 / +972 / 00972 لـ 0.
     * مثال: "+970 59-123 4567" بيصير "0591234567".
     */
    public static String normalizePhone(String phone) {
        if (phone == null) return "";
        String p = Money.normalizeDigits(phone.trim()).replaceAll("[\\s\\-()]", "");
        if (p.startsWith("+")) p = "00" + p.substring(1);
        if (p.startsWith("00970") || p.startsWith("00972")) {
            p = "0" + p.substring(5);
        }
        return p;
    }

    /** مبلغ الحركة: لازم يكون رقم صحيح، أكبر من صفر، ومش أكبر من الحد. */
    public static Result amount(String input) {
        if (input == null || input.trim().isEmpty()) return Result.EMPTY;
        String s = Money.normalizeDigits(input.trim()).replace('٫', '.').replace(',', '.');
        if (!s.matches("\\d*(\\.\\d{0,2})?") || s.equals(".") ) return Result.INVALID_FORMAT;
        String whole = s.contains(".") ? s.substring(0, s.indexOf('.')) : s;
        if (whole.length() > 9) return Result.TOO_LARGE;
        Long agorot = Money.parseToAgorot(input);
        if (agorot == null) return Result.TOO_LARGE; // بس هيك بيرجع null هون: أكبر من MAX_AGOROT
        if (agorot <= 0) return Result.NOT_POSITIVE;
        return Result.OK;
    }

    /** الوصف اختياري، بس إله حد أقصى. */
    public static Result note(String note) {
        if (note == null) return Result.OK;
        return note.trim().length() <= NOTE_MAX ? Result.OK : Result.TOO_LONG;
    }
}
