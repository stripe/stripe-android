package com.stripe.android.test.core.ui

import android.util.Log
import androidx.annotation.StringRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import java.security.InvalidParameterException

open class EspressoLabelIdButton(@StringRes val label: Int) {
    fun click() {
        var attempts = 0;
        while(attempts < 3) {
            attempts++
            try {
                Espresso.onView(ViewMatchers.withText(label))
                    .perform(ViewActions.scrollTo())
                    .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                    .perform(ViewActions.click())
            } catch (e: Exception) {
                Log.e("Stripe", "Error finding playground complete button, attempt: $attempts")
            }
        }
    }

    fun exists(): Boolean {
        return try {
            Espresso.onView(ViewMatchers.withText(label))
                .withFailureHandler { _, _ ->
                    throw InvalidParameterException("No payment selector found")
                }
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            true
        } catch (e: InvalidParameterException) {
            false
        }
    }
}
