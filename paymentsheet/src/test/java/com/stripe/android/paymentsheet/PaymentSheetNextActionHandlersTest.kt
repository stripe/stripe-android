package com.stripe.android.paymentsheet

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.paymentdatacollection.polling.PollingNextActionHandler
import org.junit.Test

class PaymentSheetNextActionHandlersTest {
    @Test
    fun `MB WAY await authorization uses polling next action handler`() {
        val handler = PaymentSheetNextActionHandlers.get()[
            StripeIntent.NextActionData.MbWayAwaitAuthorization::class.java
        ]

        assertThat(handler).isInstanceOf(PollingNextActionHandler::class.java)
    }
}
