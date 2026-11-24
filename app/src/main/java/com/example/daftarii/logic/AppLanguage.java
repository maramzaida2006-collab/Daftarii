package com.example.daftarii.logic;

/**
 * لغات التطبيق. العربي هو الأساسي، والإنجليزي التاني.
 * ما فيه Android، فبينفحص بـ JUnit. التطبيق الفعلي للغة بـ util/LanguageManager.
 */
public enum AppLanguage {
    ARABIC("ar"),
    ENGLISH("en");

    private final String tag;

    AppLanguage(String tag) {
        this.tag = tag;
    }

    /** الكود اللي بيفهمه أندرويد: "ar" أو "en". */
    public String tag() {
        return tag;
    }

    /** اللغة التانية: زر تغيير اللغة بيبدّل بينهم. */
    public AppLanguage other() {
        return this == ARABIC ? ENGLISH : ARABIC;
    }

    /**
     * من كود لغة متل "en-US" أو "ar-PS". أي لغة غير الإنجليزي بنعرضلها عربي،
     * لأنه العربي هو لغة التطبيق الأساسية.
     */
    public static AppLanguage fromTag(String tag) {
        if (tag == null) return ARABIC;
        String t = tag.trim().toLowerCase(java.util.Locale.ROOT);
        return t.equals("en") || t.startsWith("en-") || t.startsWith("en_") ? ENGLISH : ARABIC;
    }
}
