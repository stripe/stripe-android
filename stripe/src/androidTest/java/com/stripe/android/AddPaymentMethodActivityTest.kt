package com.stripe.android

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.stripe.android.view.AddPaymentMethodActivity
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddPaymentMethodActivityTest {

    @Rule
    var mActivityRule = ActivityTestRule(AddPaymentMethodActivity::class.java)

    @Test
    fun titleRenders() {
        onView(withText(R.string.title_add_a_card)).check(matches(isDisplayed()))
    }
}
