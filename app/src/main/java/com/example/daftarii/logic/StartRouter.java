package com.example.daftarii.logic;

/**
 * بيقرر وين يروح المستخدم لما يفتح التطبيق. ما فيه أي Android أو Firebase،
 * فبينفحص بـ JUnit. شاشة البداية (Splash) رح تستخدمه بالدفعة الجاية.
 */
public final class StartRouter {

    public enum Destination {
        /** شاشات الترحيب: أول مرة بس. */
        ONBOARDING,
        /** تسجيل الدخول. */
        LOGIN,
        /** حساب انعمل بس الإيميل لسا مش مفعّل. */
        VERIFY_EMAIL,
        /** حساب مفعّل: شاشة القفل (PIN أو بصمة). */
        LOCK,
        /** زائر بفترته التجريبية: الرئيسية مباشرة، بدون قفل. */
        HOME_GUEST,
        /** زائر خلصت الـ 7 أيام تبعته: لازم يعمل حساب. */
        GUEST_EXPIRED
    }

    private StartRouter() {}

    /**
     * @param onboardingDone  شاف شاشات الترحيب قبل
     * @param signedIn        في مستخدم مسجّل بـ Firebase (حساب أو زائر)
     * @param guest           المستخدم زائر (Anonymous)
     * @param emailVerified   الإيميل مفعّل (للحساب العادي)
     * @param rememberMe      علّم "تذكرني" آخر مرة
     * @param guestExpired    خلصت الفترة التجريبية للزائر
     */
    public static Destination decide(boolean onboardingDone, boolean signedIn, boolean guest,
                                     boolean emailVerified, boolean rememberMe, boolean guestExpired) {
        if (!signedIn) {
            return onboardingDone ? Destination.LOGIN : Destination.ONBOARDING;
        }
        if (guest) {
            return guestExpired ? Destination.GUEST_EXPIRED : Destination.HOME_GUEST;
        }
        if (!emailVerified) return Destination.VERIFY_EMAIL;
        // حساب مفعّل بس ما علّم "تذكرني": بنطلّعه وبيرجع يسجّل دخول
        return rememberMe ? Destination.LOCK : Destination.LOGIN;
    }

    /** لما يرجّع LOGIN لمستخدم مسجّل، لازم نعمله signOut أول. */
    public static boolean shouldSignOut(Destination destination, boolean signedIn) {
        return signedIn && destination == Destination.LOGIN;
    }
}
