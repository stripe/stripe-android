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
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class PaymentSheetEventTest {

    @Test
    fun `Init event with full config should return expected params`() {
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            isDecoupled = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_customer_googlepay"
        )

        val expectedConfig = mapOf(
            "customer" to true,
            "googlepay" to true,
            "primary_button_color" to false,
            "default_billing_details" to false,
            "allows_delayed_payment_methods" to false,
            "appearance" to mapOf(
                "colorsLight" to false,
                "colorsDark" to false,
                "corner_radius" to false,
                "border_width" to false,
                "font" to false,
                "size_scale_factor" to false,
                "primary_button" to mapOf(
                    "colorsLight" to false,
                    "colorsDark" to false,
                    "corner_radius" to false,
                    "border_width" to false,
                    "font" to false,
                ),
                "usage" to false,
            ),
            "billing_details_collection_configuration" to mapOf(
                "attach_defaults" to false,
                "name" to "Automatic",
                "email" to "Automatic",
                "phone" to "Automatic",
                "address" to "Automatic",
            ),
        )

        assertThat(event.params).run {
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
    }

    @Test
    fun `Init event with minimum config should return expected params`() {
        val event = PaymentSheetEvent.Init(
            mode = EventReporter.Mode.Complete,
            configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
            isDecoupled = false,
        )

        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_complete_init_default"
        )

        val expectedConfig = mapOf(
            "customer" to false,
            "googlepay" to false,
            "primary_button_color" to false,
            "default_billing_details" to false,
            "allows_delayed_payment_methods" to false,
            "appearance" to mapOf(
                "colorsLight" to false,
                "colorsDark" to false,
                "corner_radius" to false,
                "border_width" to false,
                "font" to false,
                "size_scale_factor" to false,
                "primary_button" to mapOf(
                    "colorsLight" to false,
                    "colorsDark" to false,
                    "corner_radius" to false,
                    "border_width" to false,
                    "font" to false,
                ),
                "usage" to false,
            ),
            "billing_details_collection_configuration" to mapOf(
                "attach_defaults" to false,
                "name" to "Automatic",
                "email" to "Automatic",
                "phone" to "Automatic",
                "address" to "Automatic",
            ),
        )

        assertThat(event.params).run {
            containsEntry("is_decoupled", false)
            containsEntry("mpe_config", expectedConfig)
        }
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
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_success"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "card",
            )
        )
    }

    @Test
    fun `Saved payment method event should return expected event`() {
        val savedPMEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            savedPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_success"
        )
        assertThat(
            savedPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "card",
            )
        )
    }

    @Test
    fun `Google pay payment method event should return expected event`() {
        val googlePayEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.GooglePay,
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            googlePayEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_success"
        )
        assertThat(
            googlePayEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "google_pay",
            )
        )
    }

    @Test
    fun `Link payment method event should return expected event`() {
        val linkEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Link,
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            linkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
        assertThat(
            linkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "link",
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
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Success,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            inlineLinkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_success"
        )
        assertThat(
            inlineLinkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
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
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            newPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_newpm_failure"
        )
        assertThat(
            newPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "card",
            )
        )
    }

    @Test
    fun `Saved payment method failure event should return expected event`() {
        val savedPMEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Saved(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            savedPMEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_savedpm_failure"
        )
        assertThat(
            savedPMEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "card",
            )
        )
    }

    @Test
    fun `Google pay payment method failure event should return expected event`() {
        val googlePayEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.GooglePay,
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            googlePayEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_googlepay_failure"
        )
        assertThat(
            googlePayEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "google_pay",
            )
        )
    }

    @Test
    fun `Link payment method failure event should return expected event`() {
        val linkEvent = PaymentSheetEvent.Payment(
            mode = EventReporter.Mode.Complete,
            paymentSelection = PaymentSelection.Link,
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            linkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )
        assertThat(
            linkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
                "selected_lpm" to "link",
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
            duration = 1.milliseconds,
            result = PaymentSheetEvent.Payment.Result.Failure,
            currency = "usd",
            isDecoupled = false,
            deferredIntentConfirmationType = null,
        )
        assertThat(
            inlineLinkEvent.eventName
        ).isEqualTo(
            "mc_complete_payment_link_failure"
        )
        assertThat(
            inlineLinkEvent.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "duration" to 0.001F,
                "is_decoupled" to false,
            )
        )
    }

    @Test
    fun `SelectPaymentOption event should return expected toString()`() {
        val event = PaymentSheetEvent.SelectPaymentOption(
            mode = EventReporter.Mode.Custom,
            paymentSelection = PaymentSelection.GooglePay,
            currency = "usd",
            isDecoupled = false,
        )
        assertThat(
            event.eventName
        ).isEqualTo(
            "mc_custom_paymentoption_googlepay_select"
        )
        assertThat(
            event.params
        ).isEqualTo(
            mapOf(
                "currency" to "usd",
                "is_decoupled" to false,
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
        val expectedBillingDetailsCollection = mapOf(
            "attach_defaults" to false,
            "name" to "Automatic",
            "email" to "Automatic",
            "phone" to "Automatic",
            "address" to "Automatic",
        )
        val expectedConfigMap = mapOf(
            "customer" to false,
            "googlepay" to false,
            "primary_button_color" to false,
            "default_billing_details" to false,
            "allows_delayed_payment_methods" to false,
            "appearance" to expectedAppearance,
            "billing_details_collection_configuration" to expectedBillingDetailsCollection,
        )
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_MINIMUM,
                isDecoupled = false,
            ).params
        ).isEqualTo(
            mapOf(
                "mpe_config" to expectedConfigMap,
                "is_decoupled" to false,
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
        val expectedBillingDetailsCollection = mapOf(
            "attach_defaults" to true,
            "name" to "Always",
            "email" to "Always",
            "phone" to "Always",
            "address" to "Full",
        )
        val expectedConfigMap = mapOf(
            "customer" to true,
            "googlepay" to true,
            "primary_button_color" to true,
            "default_billing_details" to true,
            "allows_delayed_payment_methods" to true,
            "appearance" to expectedAppearance,
            "billing_details_collection_configuration" to expectedBillingDetailsCollection,
        )
        assertThat(
            PaymentSheetEvent.Init(
                mode = EventReporter.Mode.Complete,
                configuration = PaymentSheetFixtures.CONFIG_WITH_EVERYTHING,
                isDecoupled = false,
            ).params
        ).isEqualTo(
            mapOf(
                "mpe_config" to expectedConfigMap,
                "is_decoupled" to false,
            )
        )
    }
}
