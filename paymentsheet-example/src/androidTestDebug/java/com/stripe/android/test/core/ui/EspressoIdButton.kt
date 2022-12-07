package com.stripe.android.test.core.ui

import androidx.annotation.IntegerRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import java.security.InvalidParameterException

open class EspressoIdButton(@IntegerRes val id: Int) {

    private val interaction: ViewInteraction
        get() = Espresso.onView(ViewMatchers.withId(id))

    fun click() {
        val isNotVisible = runCatching { isDisplayed() }.isFailure

        if (isNotVisible) {
            interaction.perform(ViewActions.scrollTo())
        }

        interaction.perform(ViewActions.click())
    }

    fun isEnabled() {
        interaction.check(ViewAssertions.matches(ViewMatchers.isEnabled()))
    }

    fun checkEnabled(): Boolean {
        return try {
            interaction
                .withFailureHandler { _, _ ->
                    throw InvalidParameterException("No payment selector found")
                }
                .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
            true
        } catch (e: InvalidParameterException) {
            false
        }
    }

    fun isDisplayed() {
        interaction.check(ViewAssertions.matches(ViewMatchers.isCompletelyDisplayed()))
    }
}
