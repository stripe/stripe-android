package com.stripe.android.test.core.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withText

open class EspressoText(private val text: String) {

    fun click() {
        onView(withText(text))
            .perform(ViewActions.click())
    }
}
