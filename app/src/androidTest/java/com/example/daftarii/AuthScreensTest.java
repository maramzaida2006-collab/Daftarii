package com.example.daftarii;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * شاشات الدخول وإنشاء الحساب: الفحص قبل ما نحكي مع Firebase.
 * ولا اختبار هون بيبعت طلب للإنترنت: كلهم بيوقفوا عند رسالة الخطأ.
 */
@RunWith(AndroidJUnit4.class)
public class AuthScreensTest {

    @Test
    public void login_emptyFields_showErrors() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.btnLogin)).perform(scrollTo(), click());
            onView(withText(R.string.error_email_empty)).check(matches(isDisplayed()));
            onView(withText(R.string.error_password_empty)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void login_invalidEmail_showsError() {
        try (ActivityScenario<LoginActivity> ignored = ActivityScenario.launch(LoginActivity.class)) {
            onView(withId(R.id.etLoginEmail)).perform(replaceText("not-an-email"), closeSoftKeyboard());
            onView(withId(R.id.etLoginPassword)).perform(replaceText("secret12"), closeSoftKeyboard());
            onView(withId(R.id.btnLogin)).perform(scrollTo(), click());
            onView(withText(R.string.error_email_invalid)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void register_emptyShopName_showsError() {
        try (ActivityScenario<RegisterActivity> ignored = ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.btnRegisterSubmit)).perform(scrollTo(), click());
            onView(withText(R.string.error_shop_name_empty)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void register_passwordMismatch_showsError() {
        try (ActivityScenario<RegisterActivity> ignored = ActivityScenario.launch(RegisterActivity.class)) {
            onView(withId(R.id.etRegisterName)).perform(replaceText("سوبرماركت التجربة"), closeSoftKeyboard());
            onView(withId(R.id.etRegisterEmail)).perform(replaceText("test@example.com"), closeSoftKeyboard());
            onView(withId(R.id.etRegisterPassword)).perform(replaceText("secret12"), closeSoftKeyboard());
            onView(withId(R.id.etRegisterConfirmPassword)).perform(scrollTo(), replaceText("secret13"), closeSoftKeyboard());
            onView(withId(R.id.btnRegisterSubmit)).perform(scrollTo(), click());
            onView(withText(R.string.error_password_mismatch)).perform(scrollTo()).check(matches(isDisplayed()));
        }
    }
}
