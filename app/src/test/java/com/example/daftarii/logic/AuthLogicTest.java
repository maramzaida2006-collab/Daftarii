package com.example.daftarii.logic;

import static com.example.daftarii.logic.StartRouter.Destination.GUEST_EXPIRED;
import static com.example.daftarii.logic.StartRouter.Destination.HOME_GUEST;
import static com.example.daftarii.logic.StartRouter.Destination.LOCK;
import static com.example.daftarii.logic.StartRouter.Destination.LOGIN;
import static com.example.daftarii.logic.StartRouter.Destination.ONBOARDING;
import static com.example.daftarii.logic.StartRouter.Destination.VERIFY_EMAIL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** اختبارات منطق الدخول: الأخطاء، الفترة التجريبية، قوة كلمة السر، ووين يروح المستخدم. */
public class AuthLogicTest {

    private static final long HOUR = 3_600_000L;
    private static final long DAY = 24 * HOUR;
    private static final long START = 1_795_000_000_000L;

    // ---------- AuthError ----------

    @Test
    public void authError_wrongEmailOrPassword_sameMessage() {
        assertEquals(AuthError.WRONG_CREDENTIALS, AuthError.fromFirebaseCode("ERROR_WRONG_PASSWORD"));
        assertEquals(AuthError.WRONG_CREDENTIALS, AuthError.fromFirebaseCode("ERROR_USER_NOT_FOUND"));
        assertEquals(AuthError.WRONG_CREDENTIALS, AuthError.fromFirebaseCode("ERROR_INVALID_CREDENTIAL"));
    }

    @Test
    public void authError_otherCodes() {
        assertEquals(AuthError.INVALID_EMAIL, AuthError.fromFirebaseCode("ERROR_INVALID_EMAIL"));
        assertEquals(AuthError.USER_DISABLED, AuthError.fromFirebaseCode("ERROR_USER_DISABLED"));
        assertEquals(AuthError.EMAIL_IN_USE, AuthError.fromFirebaseCode("ERROR_EMAIL_ALREADY_IN_USE"));
        assertEquals(AuthError.WEAK_PASSWORD, AuthError.fromFirebaseCode("ERROR_WEAK_PASSWORD"));
        assertEquals(AuthError.TOO_MANY_REQUESTS, AuthError.fromFirebaseCode("ERROR_TOO_MANY_REQUESTS"));
        assertEquals(AuthError.NETWORK, AuthError.fromFirebaseCode("ERROR_NETWORK_REQUEST_FAILED"));
        assertEquals(AuthError.OPERATION_NOT_ALLOWED, AuthError.fromFirebaseCode("ERROR_OPERATION_NOT_ALLOWED"));
    }

    @Test
    public void authError_unknownOrNull() {
        assertEquals(AuthError.UNKNOWN, AuthError.fromFirebaseCode("SOMETHING_NEW"));
        assertEquals(AuthError.UNKNOWN, AuthError.fromFirebaseCode(null));
    }

    // ---------- GuestTrial ----------

    @Test
    public void guestTrial_firstDay_sevenLeft() {
        assertEquals(7, GuestTrial.daysLeft(START, START));
        assertEquals(7, GuestTrial.daysLeft(START, START + 5 * HOUR));
    }

    @Test
    public void guestTrial_partialDayCountsAsFullDay() {
        // فات 5 أيام و 18 ساعة: ضايل 30 ساعة = يومين
        assertEquals(2, GuestTrial.daysLeft(START, START + 5 * DAY + 18 * HOUR));
    }

    @Test
    public void guestTrial_lastDay_oneLeft() {
        assertEquals(1, GuestTrial.daysLeft(START, START + 6 * DAY + 23 * HOUR));
        assertFalse(GuestTrial.isExpired(START, START + 6 * DAY + 23 * HOUR));
    }

    @Test
    public void guestTrial_afterSevenDays_expired() {
        assertEquals(0, GuestTrial.daysLeft(START, START + 7 * DAY));
        assertTrue(GuestTrial.isExpired(START, START + 7 * DAY));
        assertTrue(GuestTrial.isExpired(START, START + 30 * DAY));
    }

    @Test
    public void guestTrial_clockBeforeStart_notExpired() {
        assertEquals(7, GuestTrial.daysLeft(START, START - DAY));
    }

    @Test
    public void guestTrial_unknownStart_treatedAsNew() {
        assertEquals(7, GuestTrial.daysLeft(0, START));
    }

    // ---------- PasswordStrength ----------

    @Test
    public void passwordStrength_levels() {
        assertEquals(PasswordStrength.Level.NONE, PasswordStrength.level(""));
        assertEquals(PasswordStrength.Level.WEAK, PasswordStrength.level("123"));
        assertEquals(PasswordStrength.Level.WEAK, PasswordStrength.level("123456"));
        assertEquals(PasswordStrength.Level.MEDIUM, PasswordStrength.level("abc123"));
        assertEquals(PasswordStrength.Level.STRONG, PasswordStrength.level("Daftari2026"));
        assertEquals(PasswordStrength.Level.STRONG, PasswordStrength.level("shop#2026xx"));
    }

    @Test
    public void passwordStrength_shortPassword_neverAboveWeak() {
        assertEquals(1, PasswordStrength.score("aB#1"));
    }

    @Test
    public void passwordStrength_scoreRange() {
        assertEquals(0, PasswordStrength.score(null));
        assertEquals(4, PasswordStrength.score("Abcdef1234!"));
    }

    // ---------- Validators: تأكيد كلمة السر واسم المحل ----------

    @Test
    public void passwordConfirmation() {
        assertEquals(Validators.Result.OK, Validators.passwordConfirmation("secret1", "secret1"));
        assertEquals(Validators.Result.MISMATCH, Validators.passwordConfirmation("secret1", "secret2"));
        assertEquals(Validators.Result.MISMATCH, Validators.passwordConfirmation("secret1", "secret1 "));
        assertEquals(Validators.Result.EMPTY, Validators.passwordConfirmation("secret1", ""));
    }

    @Test
    public void shopName() {
        assertEquals(Validators.Result.OK, Validators.shopName("سوبرماركت النور"));
        assertEquals(Validators.Result.EMPTY, Validators.shopName("  "));
    }

    // ---------- StartRouter ----------

    @Test
    public void router_firstLaunch_onboarding() {
        assertEquals(ONBOARDING, StartRouter.decide(false, false, false, false, false, false));
    }

    @Test
    public void router_notSignedIn_login() {
        assertEquals(LOGIN, StartRouter.decide(true, false, false, false, false, false));
    }

    @Test
    public void router_guest() {
        assertEquals(HOME_GUEST, StartRouter.decide(true, true, true, false, false, false));
        assertEquals(GUEST_EXPIRED, StartRouter.decide(true, true, true, false, false, true));
    }

    @Test
    public void router_unverifiedAccount_verifyEmail() {
        assertEquals(VERIFY_EMAIL, StartRouter.decide(true, true, false, false, true, false));
    }

    @Test
    public void router_verifiedAccount() {
        assertEquals(LOCK, StartRouter.decide(true, true, false, true, true, false));
        assertEquals(LOGIN, StartRouter.decide(true, true, false, true, false, false));
    }

    @Test
    public void router_signOutOnlyWhenSignedInAndSentToLogin() {
        assertTrue(StartRouter.shouldSignOut(LOGIN, true));
        assertFalse(StartRouter.shouldSignOut(LOGIN, false));
        assertFalse(StartRouter.shouldSignOut(LOCK, true));
    }
}
