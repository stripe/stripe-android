package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.ViewState
import com.stripe.android.paymentsheet.ui.BuyButton
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BuyButtonTest {
    private val buyButton = BuyButton(ApplicationProvider.getApplicationContext())

    @Test
    fun `disabling button should update label alpha`() {
        buyButton.isEnabled = false
        assertThat(buyButton.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `onReadyState() should update label`() {
        buyButton.onReadyState(
            ViewState.Ready(amount = 1099, currencyCode = "usd")
        )
        assertThat(
            buyButton.label.text.toString()
        ).isEqualTo(
            "Pay $10.99"
        )
    }

    @Test
    fun `onConfirmingState() should update label`() {
        buyButton.onConfirmingState()
        assertThat(
            buyButton.label.text.toString()
        ).isEqualTo(
            "Processing…"
        )
    }
}
