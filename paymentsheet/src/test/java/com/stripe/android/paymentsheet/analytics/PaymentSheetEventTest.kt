package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class PaymentSheetEventTest {

    @Test
    fun `Init event with full config should return expected toString()`() {
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
            ).eventName
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
            ).eventName
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
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_success"
        )
    }

    @Test
    fun `SelectPaymentOption event should return expected toString()`() {
        assertThat(
            PaymentSheetEvent.SelectPaymentOption(
                mode = EventReporter.Mode.Custom,
                paymentSelection = PaymentSelection.GooglePay
            ).eventName
        ).isEqualTo(
            "mc_custom_paymentoption_googlepay_select"
        )
    }

    @Test
    fun `Init event should have default params if config is all defaults`() {
        val expectedConfigMap = mapOf(
            "customer" to false,
            "googlepay" to false,
            "primary_button_color" to false,
            "default_billing_details" to false,
            "allows_delayed_payment_methods" to false,
        )
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_MINIMUM
            ).additionalParams
        ).isEqualTo(
            mapOf("payment_sheet_configuration" to expectedConfigMap)
        )
    }

    @Test
    fun `Init event should should mark all optional params present if there are there`() {
        val expectedConfigMap = mapOf(
            "customer" to true,
            "googlepay" to true,
            "primary_button_color" to true,
            "default_billing_details" to true,
            "allows_delayed_payment_methods" to true,
        )
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
            ).additionalParams
        ).isEqualTo(
            mapOf("payment_sheet_configuration" to expectedConfigMap)
        )
    }
}
