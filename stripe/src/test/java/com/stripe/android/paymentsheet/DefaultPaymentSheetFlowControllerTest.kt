package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.PaymentOption
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class DefaultPaymentSheetFlowControllerTest {

    @Test
    fun `presentPaymentOptions() should call onComplete() with null`() {
        val flowController = DefaultPaymentSheetFlowController(
            DefaultPaymentSheetFlowController.Args.Default(
                "client_secret",
                "ephkey",
                "cus_123"
            ),
            paymentMethodTypes = listOf(PaymentMethod.Type.Card),
            paymentMethods = emptyList(),
            defaultPaymentMethodId = null
        )

        var paymentOption: PaymentOption? = null
        flowController.presentPaymentOptions(mock()) {
            paymentOption = it
        }

        assertThat(paymentOption)
            .isNull()
    }
}
