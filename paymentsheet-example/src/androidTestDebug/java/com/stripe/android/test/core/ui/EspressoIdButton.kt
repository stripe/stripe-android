package com.stripe.android.test.core.ui

import android.content.Context
import androidx.annotation.IntegerRes
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.common.truth.Truth.assertThat
import java.security.InvalidParameterException

open class EspressoIdButton(@IntegerRes val id: Int) {

    private val interaction: ViewInteraction
        get() = Espresso.onView(ViewMatchers.withId(id))

    fun click() {
        scrollToIfNeeded()
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

    fun waitForEnabled() {
        scrollToIfNeeded()
        val resources = ApplicationProvider.getApplicationContext<Context>().resources
        val resourceIdName = resources.getResourceEntryName(id)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = UiSelector().resourceIdMatches(".*/$resourceIdName").enabled(true)
        assertThat(device.findObject(selector).waitForExists(10_000)).isTrue()
    }

    private fun scrollToIfNeeded() {
        val isNotVisible = runCatching { isDisplayed() }.isFailure

        if (isNotVisible) {
            interaction.perform(ViewActions.scrollTo())
        }
    }
}
