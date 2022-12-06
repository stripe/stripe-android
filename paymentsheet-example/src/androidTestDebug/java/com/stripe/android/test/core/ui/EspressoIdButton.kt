package com.stripe.android.test.core.ui

import androidx.annotation.IntegerRes
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import java.security.InvalidParameterException

open class EspressoIdButton(@IntegerRes val id: Int) {

    fun click() {
        val interaction = Espresso.onView(ViewMatchers.withId(id))

        if (interaction.isNotVisible) {
            interaction.perform(ViewActions.scrollTo())
        }

        interaction.perform(ViewActions.click())
    }

    fun isEnabled() {
        Espresso.onView(ViewMatchers.withId(id))
            .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
    }

    fun checkEnabled(): Boolean {
        return try {
            Espresso.onView(ViewMatchers.withId(id))
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
        Espresso.onView(ViewMatchers.withId(id))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }
}

private val ViewInteraction.isNotVisible: Boolean
    get() = runCatching { check(ViewAssertions.matches(ViewMatchers.isDisplayed())) }.isFailure
