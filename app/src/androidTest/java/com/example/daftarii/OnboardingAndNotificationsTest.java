package com.example.daftarii;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.not;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/** الترحيب (التالي، تخطي) وشاشة الإشعارات (التبويبات). */
@RunWith(AndroidJUnit4.class)
public class OnboardingAndNotificationsTest {

    /** "تخطي" بيحفظ إنه شاف الترحيب: بنرجّعه عشان ما يأثر على تشغيل التطبيق بعد الاختبار. */
    @After
    public void resetSession() {
        Context c = ApplicationProvider.getApplicationContext();
        c.getSharedPreferences("daftari_session", Context.MODE_PRIVATE)
                .edit().remove("onboarding_done").apply();
    }

    @Test
    public void onboarding_next_showsStartButton() {
        try (ActivityScenario<OnboardingActivity> ignored = ActivityScenario.launch(OnboardingActivity.class)) {
            onView(withId(R.id.btnStart)).check(matches(not(isDisplayed())));
            onView(withId(R.id.btnNext)).perform(click());
            onView(withId(R.id.btnStart)).check(matches(isDisplayed()));
            onView(withId(R.id.btnNext)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void onboarding_skip_opensLogin() {
        try (ActivityScenario<OnboardingActivity> ignored = ActivityScenario.launch(OnboardingActivity.class)) {
            onView(withId(R.id.btnSkip)).perform(click());
            onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void notifications_tabsSwitch() {
        try (ActivityScenario<NotificationsActivity> ignored = ActivityScenario.launch(NotificationsActivity.class)) {
            onView(withId(R.id.tabAll)).check(matches(isSelected()));
            onView(withId(R.id.tabSystem)).perform(click());
            onView(withId(R.id.tabSystem)).check(matches(isSelected()));
            onView(withId(R.id.tabAll)).check(matches(not(isSelected())));
            onView(withId(R.id.tabPayments)).perform(click());
            onView(withId(R.id.tabPayments)).check(matches(isSelected()));
        }
    }
}
