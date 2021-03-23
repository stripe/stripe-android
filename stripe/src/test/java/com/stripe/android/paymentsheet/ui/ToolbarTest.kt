package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.R
import com.stripe.android.view.ActivityScenarioFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class ToolbarTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val toolbar: Toolbar by lazy {
        activityScenarioFactory.createView {
            Toolbar(it)
        }
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(context, ApiKeyFixtures.FAKE_PUBLISHABLE_KEY)
    }

    @Test
    fun `toolbar is enabled by default`() {
        assertThat(toolbar.isEnabled)
            .isTrue()
    }

    @Test
    fun `button is disabled when processing`() {
        var count = 0
        toolbar.action.observeForever {
            count++
        }

        toolbar.updateProcessing(true)
        assertThat(toolbar.isEnabled)
            .isFalse()

        onView(withContentDescription(R.string.stripe_paymentsheet_close)).perform(click())
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun `action emits Close when close button is clicked`() {
        var action: Toolbar.Action? = null
        toolbar.action.observeForever {
            action = it
        }
        // Should show close by default
        onView(withContentDescription(R.string.stripe_paymentsheet_close)).perform(click())
        assertThat(action)
            .isEqualTo(Toolbar.Action.Close)
    }

    @Test
    fun `action emits Back when back button is clicked`() {
        var action: Toolbar.Action? = null
        toolbar.action.observeForever {
            action = it
        }
        toolbar.showBack()
        onView(withContentDescription(R.string.stripe_paymentsheet_back)).perform(click())
        assertThat(action)
            .isEqualTo(Toolbar.Action.Back)
    }
}
