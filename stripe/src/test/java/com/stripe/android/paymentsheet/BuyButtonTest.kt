package com.stripe.android.paymentsheet

import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.model.ViewState
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class BuyButtonTest {
    private val buyButton = BuyButton(ApplicationProvider.getApplicationContext())

    @Test
    fun `disabling button should update label alpha`() {
        buyButton.isEnabled = false
        assertThat(buyButton.viewBinding.label.alpha)
            .isEqualTo(0.5f)
    }

    @Test
    fun `updateState() with ready state should update label`() {
        buyButton.updateState(
            ViewState.Ready(amount = 1099, currencyCode = "usd")
        )
        assertThat(
            buyButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Pay $10.99"
        )
    }

    @Test
    fun `updateState() with confirming state should update label`() {
        buyButton.updateState(ViewState.Confirming)
        assertThat(
            buyButton.viewBinding.label.text.toString()
        ).isEqualTo(
            "Processing…"
        )
    }
}
