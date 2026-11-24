package com.example.daftarii.logic;

/**
 * تحويل المبالغ بين النص اللي بيكتبه المستخدم والأغورة (long).
 * مثال: "45.50" بتصير 4550، و 4550 بتنعرض "45.50".
 *
 * ليش long؟ لأن double بيعمل أخطاء كسور (0.1 + 0.2 != 0.3).
 */
public final class Money {

    /** أكبر مبلغ بنقبله بحركة وحدة: مليون شيكل. */
    public static final long MAX_AGOROT = 100_000_000L;

    private Money() {}

    /**
     * بيقرأ مبلغ من نص المستخدم. بيقبل أرقام عربية (٠-٩) وإنجليزية،
     * والفاصلة العشرية "." أو "," أو "٫"، ولحد خانتين بعد الفاصلة.
     *
     * @return المبلغ بالأغورة، أو null إذا النص مش مبلغ صحيح.
     *         ما بيفحص إنه أكبر من صفر: هاد شغل Validators.
     */
    public static Long parseToAgorot(String input) {
        if (input == null) return null;
        String s = normalizeDigits(input.trim())
                .replace('٫', '.')
                .replace(',', '.');
        if (!s.matches("\\d{1,9}(\\.\\d{0,2})?") && !s.matches("\\.\\d{1,2}")) {
            return null;
        }
        String[] parts = s.split("\\.", -1);
        long whole = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
        long fraction = 0;
        if (parts.length > 1 && !parts[1].isEmpty()) {
            String f = parts[1].length() == 1 ? parts[1] + "0" : parts[1];
            fraction = Long.parseLong(f);
        }
        long agorot = whole * 100 + fraction;
        if (agorot > MAX_AGOROT) return null;
        return agorot;
    }

    /** بيعرض المبلغ بخانتين: 4550 بتصير "45.50"، و -300 بتصير "-3.00". */
    public static String format(long agorot) {
        String sign = agorot < 0 ? "-" : "";
        long abs = Math.abs(agorot);
        long fraction = abs % 100;
        return sign + (abs / 100) + "." + (fraction < 10 ? "0" : "") + fraction;
    }

    /** بيحوّل الأرقام العربية (٠١٢٣٤٥٦٧٨٩) والفارسية لأرقام إنجليزية. */
    public static String normalizeDigits(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c >= '٠' && c <= '٩') {
                sb.append((char) ('0' + (c - '٠')));
            } else if (c >= '۰' && c <= '۹') {
                sb.append((char) ('0' + (c - '۰')));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
