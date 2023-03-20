package com.stripe.android.test.core.ui

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.matcher.ViewMatchers.withId

class EspressoEditText(
    @IdRes private val id: Int,
) {
    fun enter(text: String) {
        onView(withId(id)).perform(typeText(text))
        closeSoftKeyboard()
    }
}
