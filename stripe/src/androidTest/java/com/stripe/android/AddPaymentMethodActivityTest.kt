package com.stripe.android

import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4

import com.stripe.android.view.AddPaymentMethodActivity

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withText

@RunWith(AndroidJUnit4::class)
class AddPaymentMethodActivityTest {

    @Rule
    var mActivityRule = ActivityTestRule(AddPaymentMethodActivity::class.java)

    @Test
    fun titleRenders() {
        onView(withText(R.string.title_add_a_card)).check(matches(isDisplayed()))
    }
}
