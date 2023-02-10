package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.model.PaymentDetailsFixtures
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodFixtures
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
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_customer_googlepay"
        )
        assertThat(
            event.additionalParams
        ).containsEntry("locale", "en_US")
    }

    @Test
    fun `Init event with minimum config should return expected toString()`() {
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_default"
        )
        assertThat(
            event.additionalParams
        ).containsEntry("locale", "en_US")
    }

    @Test
    fun `New payment method event should return expected event`() {
        val newPMEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                mock(),
                mock()
            ),
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd"
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(
            newPMEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Saved payment method event should return expected event`() {
        val savedPMEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd"
        )
        assertThat(
            savedPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_success"
        )
        assertThat(
            savedPMEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Google pay payment method event should return expected event`() {
        val googlePayEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.GooglePay,
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd"
        )
        assertThat(
            googlePayEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_success"
        )
        assertThat(
            googlePayEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Link payment method event should return expected event`() {
        val linkEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Link,
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd"
        )
        assertThat(
            linkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
        assertThat(
            linkEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Inline Link payment method event should return expected event`() {
        val inlineLinkEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.New.LinkInline(
                LinkPaymentDetails.New(
                    PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                    mock(),
                    mock()
                )
            ),
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd"
        )
        assertThat(
            inlineLinkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
        assertThat(
            inlineLinkEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `New payment method failure event should return expected event`() {
        val newPMEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.New.Card(
                PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                mock(),
                mock()
            ),
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd"
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(
            newPMEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Saved payment method failure event should return expected event`() {
        val savedPMEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd"
        )
        assertThat(
            savedPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_failure"
        )
        assertThat(
            savedPMEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Google pay payment method failure event should return expected event`() {
        val googlePayEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.GooglePay,
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd"
        )
        assertThat(
            googlePayEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_failure"
        )
        assertThat(
            googlePayEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Link payment method failure event should return expected event`() {
        val linkEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Link,
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd"
        )
        assertThat(
            linkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )
        assertThat(
            linkEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `Inline Link payment method failure event should return expected event`() {
        val inlineLinkEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.New.LinkInline(
                LinkPaymentDetails.New(
                    PaymentDetailsFixtures.CONSUMER_SINGLE_PAYMENT_DETAILS.paymentDetails.first(),
                    mock(),
                    mock()
                )
            ),
            durationMillis = 1L,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd"
        )
        assertThat(
            inlineLinkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )
        assertThat(
            inlineLinkEvent.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
                "duration" to 0.001F
            )
        )
    }

    @Test
    fun `SelectPaymentOption event should return expected toString()`() {
        val event = PaymentSheetEvent.SelectPaymentOption(
            mode = EventReporter.Mode.Custom,
            paymentSelection = PaymentSelection.GooglePay,
            currency = "usd"
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_custom_paymentoption_googlepay_select"
        )
        assertThat(
            event.additionalParams
        ).isEqualTo(
            mapOf(
                "locale" to "en_US",
                "currency" to "usd",
            )
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
            mapOf(
                "mpe_config" to expectedConfigMap,
                "locale" to "en_US"
            )
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
            mapOf(
                "mpe_config" to expectedConfigMap,
                "locale" to "en_US"
            )
        )
    }
}
