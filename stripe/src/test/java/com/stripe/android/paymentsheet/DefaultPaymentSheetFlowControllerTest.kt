package com.stripe.android.paymentsheet

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetFlowControllerTest {

    private val flowController = DefaultPaymentSheetFlowController(
        DefaultPaymentSheetFlowController.Args.Default(
            "client_secret",
            "ephkey",
            "cus_123"
        ),
        emptyList(),
        null
    )

    @Test
    fun `presentPaymentOptions() should call onComplete() with null`() {
        var paymentOption: PaymentOption? = null
        flowController.presentPaymentOptions(mock()) {
            paymentOption = it
        }

        assertThat(paymentOption)
            .isNull()
    }

    @Test
    fun `onPaymentOptionResult() with saved payment method selection result should return payment option`() {
        val paymentOption = flowController.onPaymentOptionResult(
            Intent().putExtras(
                PaymentOptionResult.Succeeded(
                    PaymentSelection.Saved("pm_123")
                ).toBundle()
            )
        )
        assertThat(paymentOption)
            .isEqualTo(
                PaymentOption(
                    drawableResourceId = R.drawable.stripe_ic_visa,
                    label = CardBrand.Visa.displayName
                )
            )
    }

    @Test
    fun `onPaymentOptionResult() with cancelled result should return null`() {
        val paymentOption = flowController.onPaymentOptionResult(
            Intent().putExtras(
                PaymentOptionResult.Cancelled(null).toBundle()
            )
        )
        assertThat(paymentOption)
            .isNull()
    }
}
