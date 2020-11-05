package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PaymentSheetFlowControllerTest {

    @Test
    fun `create() should return an instance`() {
        var paymentSheetFlowController: PaymentSheetFlowController? = null
        PaymentSheetFlowController.create("client_secret") {
            paymentSheetFlowController = it
        }

        assertThat(paymentSheetFlowController)
            .isNotNull()
    }
}
