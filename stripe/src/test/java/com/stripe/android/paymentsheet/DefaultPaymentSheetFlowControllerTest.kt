package com.stripe.android.paymentsheet

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.stripe.android.ApiKeyFixtures
import com.stripe.android.PaymentController
import com.stripe.android.R
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetFlowControllerTest {
    private val paymentController = mock<PaymentController>()
    private val flowController = DefaultPaymentSheetFlowController(
        paymentController,
        ApiKeyFixtures.FAKE_PUBLISHABLE_KEY,
        null,
        DefaultPaymentSheetFlowController.Args.Default(
            "client_secret",
            "ephkey",
            "cus_123"
        ),
        paymentMethodTypes = listOf(PaymentMethod.Type.Card),
        paymentMethods = emptyList(),
        googlePayConfig = PaymentSheetGooglePayConfigFixtures.DEFAULT,
        defaultPaymentMethodId = null
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
                    PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
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

    @Test
    fun `confirmPayment() without paymentSelection should not call paymentController`() {
        verifyNoMoreInteractions(paymentController)
        flowController.confirmPayment(mock()) {
        }
    }
}
