package com.example.daftarii.logic;

/**
 * قوة كلمة السر لمؤشر شاشة "إنشاء حساب" (4 خطوط).
 * هاد للمساعدة بس: الشرط الإجباري الوحيد هو 6 أحرف (Validators.password).
 */
public final class PasswordStrength {

    public enum Level { NONE, WEAK, MEDIUM, STRONG }

    private PasswordStrength() {}

    /** نقاط من 0 لـ 4: كل نقطة بتعبّي خط من المؤشر. */
    public static int score(String password) {
        if (password == null || password.isEmpty()) return 0;
        int score = 0;
        if (password.length() >= Validators.PASSWORD_MIN) score++;
        if (password.length() >= 10) score++;

        boolean hasLetter = false, hasDigit = false, hasUpper = false, hasLower = false, hasSymbol = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isLetter(c)) {
                hasLetter = true;
                if (Character.isUpperCase(c)) hasUpper = true;
                if (Character.isLowerCase(c)) hasLower = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isWhitespace(c)) {
                hasSymbol = true;
            }
        }
        if (hasLetter && hasDigit) score++;
        if (hasSymbol || (hasUpper && hasLower)) score++;

        // كلمة سر أقصر من 6 ما بتطلع أكتر من "ضعيفة"
        if (password.length() < Validators.PASSWORD_MIN) score = Math.min(score, 1);
        return Math.max(score, 1);
    }

    public static Level level(String password) {
        if (password == null || password.isEmpty()) return Level.NONE;
        int s = score(password);
        if (s <= 1) return Level.WEAK;
        if (s == 2) return Level.MEDIUM;
        return Level.STRONG;
    }
}
