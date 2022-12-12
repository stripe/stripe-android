package com.stripe.android.test.core.ui

import androidx.annotation.StringRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText

open class EspressoLabelIdButton(@StringRes val label: Int) {

    fun click() {
        val interaction = onView(withText(label))
        val isNotVisible = runCatching { interaction.check(matches(isCompletelyDisplayed())) }.isFailure

        if (isNotVisible) {
            interaction.perform(scrollTo())
        }

        interaction.perform(ViewActions.click())
    }
}
