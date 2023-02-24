package com.stripe.android.test.core.ui

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers

open class EspressoText(private val text: String) {

    fun click(){
        Espresso.onView(ViewMatchers.withText(text))
            .perform(ViewActions.click())
    }
}
