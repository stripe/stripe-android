package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlin.test.Test

class PaymentSheetEventTest {

    @Test
    fun `Init event with full config should return expected toString()`() {
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ).toString()
        ).isEqualTo(
            "mc_complete_init_customer_googlepay"
        )
    }

    @Test
    fun `Init event with minimum config should return expected toString()`() {
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_MINIMUM
            ).toString()
        ).isEqualTo(
            "mc_complete_init_default"
        )
    }

    @Test
    fun `Payment event should return expected toString()`() {
        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.GooglePay,
                result = PaymentSheetEvent.Payment.Result.Success
            ).toString()
        ).isEqualTo(
            "mc_complete_payment_googlepay_success"
        )
    }
}
