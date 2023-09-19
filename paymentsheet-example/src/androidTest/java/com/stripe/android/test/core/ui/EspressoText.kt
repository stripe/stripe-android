package com.stripe.android.test.core.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

open class EspressoText(text: String) {
    private val interaction = onView(withText(text))

    fun click() {
        val isNotVisible = runCatching {
            interaction.check(matches(isCompletelyDisplayed()))
        }.isFailure

        if (isNotVisible) {
            interaction.perform(scrollTo())
        }

        interaction.perform(ViewActions.click())
    }
}
