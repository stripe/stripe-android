package com.stripe.android.test.core.ui

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import java.security.InvalidParameterException

open class EspressoLabelIdButton(@StringRes val label: Int) {
    fun click() {
        Espresso.onView(ViewMatchers.withText(label))
            .perform(ViewActions.scrollTo())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())
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
