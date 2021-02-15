package com.stripe.android.paymentsheet

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.view.ActivityScenarioFactory
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BuyButtonTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val activityScenarioFactory = ActivityScenarioFactory(context)
    private val buyButton: BuyButton by lazy {
        activityScenarioFactory.createView {
            BuyButton(it)
        }
    }

    @BeforeTest
    fun setup() {
        PaymentConfiguration.init(
            context,
            ApiKeyFixtures.FAKE_PUBLISHABLE_KEY
        )
    }

    @Test
    fun `onReadyState() should update label`() {
        buyButton.onReadyState(
            ViewState.Ready(amount = 1099, currencyCode = "usd")
        )
        assertThat(
            buyButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Pay $10.99"
        )
    }

    @Test
    fun `onConfirmingState() should update label`() {
        buyButton.onConfirmingState()
        assertThat(
            buyButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Processing…"
        )
    }

    @Test
    fun `label alpha is initially 50%`() {
        assertThat(buyButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and disabled, label alpha is 50%`() {
        buyButton.onReadyState(
            ViewState.Ready(amount = 1099, currencyCode = "usd")
        )
        assertThat(buyButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `after viewState ready and enabled, label alpha is 100%`() {
        buyButton.onReadyState(
            ViewState.Ready(amount = 1099, currencyCode = "usd")
        )
        buyButton.isEnabled = true
        assertThat(buyButton.viewBinding.label.alpha)
            .isEqualTo(1.0f)
    }
}
