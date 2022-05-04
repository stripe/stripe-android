package com.stripe.android.paymentsheet.analytics

import com.stripe.android.core.networking.AnalyticsEvent
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class PaymentSheetEvent : AnalyticsEvent {
    abstract val additionalParams: Map<String, Any>

    class Init(
        private val mode: EventReporter.Mode,
        private val configuration: PaymentSheet.Configuration?
    ) : PaymentSheetEvent() {
        override val eventName: String
            get() {
                val configValue = listOfNotNull(
                    FIELD_CUSTOMER.takeIf { configuration?.customer != null },
                    FIELD_GOOGLE_PAY.takeIf { configuration?.googlePay != null }
                ).takeUnless { it.isEmpty() }?.joinToString(separator = "_") ?: "default"
                return formatEventName(mode, "init_$configValue")
            }
        override val additionalParams: Map<String, Any>
            get() {
                val configurationMap = mapOf(
                    // todo skyler: add project wardrobe configs here too.
                    FIELD_CUSTOMER to (configuration?.customer != null),
                    FIELD_GOOGLE_PAY to (configuration?.googlePay != null),
                    FIELD_PRIMARY_BUTTON_COLOR to (configuration?.primaryButtonColor != null),
                    FIELD_BILLING to (configuration?.defaultBillingDetails != null),
                    FIELD_DELAYED_PMS to (configuration?.allowsDelayedPaymentMethods),
                )
                return mapOf(FIELD_PAYMENT_SHEET_CONFIGURATION to configurationMap)
            }
    }

    class Dismiss(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "dismiss")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class ShowNewPaymentOptionForm(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_newpm_show")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class ShowExistingPaymentOptions(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent() {
        override val eventName: String = formatEventName(mode, "sheet_savedpm_show")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class SelectPaymentOption(
        mode: EventReporter.Mode,
        paymentSelection: PaymentSelection?
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "paymentoption_${analyticsValue(paymentSelection)}_select")
        override val additionalParams: Map<String, Any> = mapOf()
    }

    class Payment(
        mode: EventReporter.Mode,
        result: Result,
        paymentSelection: PaymentSelection?
    ) : PaymentSheetEvent() {
        override val eventName: String =
            formatEventName(mode, "payment_${analyticsValue(paymentSelection)}_$result")
        override val additionalParams: Map<String, Any> = mapOf()

        enum class Result(private val code: String) {
            Success("success"),
            Failure("failure");

            override fun toString(): String = code
        }
    }

    internal companion object {
        private fun analyticsValue(
            paymentSelection: PaymentSelection?
        ) = when (paymentSelection) {
            PaymentSelection.GooglePay -> "googlepay"
            is PaymentSelection.Saved -> "savedpm"
            is PaymentSelection.New -> "newpm"
            else -> "unknown"
        }

        private fun formatEventName(mode: EventReporter.Mode, eventName: String): String {
            return "mc_${mode}_$eventName"
        }

        const val FIELD_CUSTOMER = "customer"
        const val FIELD_GOOGLE_PAY = "googlepay"
        const val FIELD_PRIMARY_BUTTON_COLOR = "primary_button_color"
        const val FIELD_BILLING = "default_billing_details"
        const val FIELD_DELAYED_PMS = "allows_delayed_payment_methods"
        const val FIELD_PAYMENT_SHEET_CONFIGURATION = "payment_sheet_configuration"
    }
}
