package com.stripe.android.paymentsheet.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
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
    fun `buttons are enabled by default`() {
        assertThat(toolbar.closeButton.isEnabled)
            .isTrue()
        assertThat(toolbar.backButton.isEnabled)
            .isTrue()
    }

    @Test
    fun `buttons are disabled when processing`() {
        toolbar.updateProcessing(true)
        assertThat(toolbar.closeButton.isEnabled)
            .isFalse()
        assertThat(toolbar.backButton.isEnabled)
            .isFalse()
    }

    @Test
    fun `action emits Back when back button is clicked`() {
        var action: Toolbar.Action? = null
        toolbar.action.observeForever {
            action = it
        }
        toolbar.backButton.performClick()
        assertThat(action)
            .isEqualTo(Toolbar.Action.Back)
    }
}
