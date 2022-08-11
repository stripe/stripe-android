package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.PaymentDetailsFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
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
                paymentSelection = PaymentSelection.New.Card(PaymentMethodCreateParamsFixtures.DEFAULT_CARD, mock(), mock()),
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Success
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.Saved(mock()),
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Success
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_success"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.GooglePay,
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Success
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_success"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.Link,
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Success
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.New.LinkInline(
                    LinkPaymentDetails.New(
                        PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                        mock(),
                        mock()
                    )
                ),
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Success
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
    }

    @Test
    fun `Payment failure event should return expected toString()`() {
        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.New.Card(PaymentMethodCreateParamsFixtures.DEFAULT_CARD, mock(), mock()),
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Failure
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.Saved(mock()),
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Failure
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_failure"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.GooglePay,
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Failure
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_failure"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.Link,
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Failure
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )

        assertThat(
            PaymentSheetEvent.Payment(
                mode = EventReporter.Mode.Complete,
                paymentSelection = PaymentSelection.New.LinkInline(
                    LinkPaymentDetails.New(
                        PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                        mock(),
                        mock()
                    )
                ),
                durationMillis = 1L,
                result = PaymentSheetEvent.Payment.Result.Failure
            ).eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
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
        val expectedPrimaryButton = mapOf(
            "colorsLight" to false,
            "colorsDark" to false,
            "corner_radius" to false,
            "border_width" to false,
            "font" to false
        )
        val expectedAppearance = mapOf(
            "colorsLight" to false,
            "colorsDark" to false,
            "corner_radius" to false,
            "border_width" to false,
            "size_scale_factor" to false,
            "font" to false,
            "primary_button" to expectedPrimaryButton,
            "usage" to false
        )
        val expectedConfigMap = mapOf(
            "customer" to false,
            "googlepay" to false,
            "primary_button_color" to false,
            "default_billing_details" to false,
            "allows_delayed_payment_methods" to false,
            "appearance" to expectedAppearance
        )
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_MINIMUM
            ).additionalParams
        ).isEqualTo(
            mapOf("mpe_config" to expectedConfigMap)
        )
    }

    @Test
    fun `Init event should should mark all optional params present if they are there`() {
        val expectedPrimaryButton = mapOf(
            "colorsLight" to true,
            "colorsDark" to true,
            "corner_radius" to true,
            "border_width" to true,
            "font" to true
        )
        val expectedAppearance = mapOf(
            "colorsLight" to true,
            "colorsDark" to true,
            "corner_radius" to true,
            "border_width" to true,
            "size_scale_factor" to true,
            "font" to true,
            "primary_button" to expectedPrimaryButton,
            "usage" to true
        )
        val expectedConfigMap = mapOf(
            "customer" to true,
            "googlepay" to true,
            "primary_button_color" to true,
            "default_billing_details" to true,
            "allows_delayed_payment_methods" to true,
            "appearance" to expectedAppearance
        )
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING
            ).additionalParams
        ).isEqualTo(
            mapOf("mpe_config" to expectedConfigMap)
        )
    }
}
