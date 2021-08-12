package com.stripe.android.paymentsheet.analytics

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal sealed class PaymentSheetEvent(
    private val mode: EventReporter.Mode
) {
    abstract val event: String

    class Init(
        mode: EventReporter.Mode,
        private val configuration: PaymentSheet.Configuration?
    ) : PaymentSheetEvent(mode) {
        override val event: String
            get() {
                val configValue = listOfNotNull(
                    "customer".takeIf { configuration?.customer != null },
                    "googlepay".takeIf { configuration?.googlePay != null }
                ).takeUnless { it.isEmpty() }?.joinToString(separator = "_") ?: "default"
                return "init_$configValue"
            }
    }

    class Dismiss(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent(mode) {
        override val event: String = "dismiss"
    }

    class ShowNewPaymentOptionForm(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent(mode) {
        override val event: String = "sheet_newpm_show"
    }

    class ShowExistingPaymentOptions(
        mode: EventReporter.Mode
    ) : PaymentSheetEvent(mode) {
        override val event: String = "sheet_savedpm_show"
    }

    class SelectPaymentOption(
        mode: EventReporter.Mode,
        paymentSelection: PaymentSelection?
    ) : PaymentSheetEvent(mode) {
        override val event: String = "paymentoption_${analyticsValue(paymentSelection)}_select"
    }

    class Payment(
        mode: EventReporter.Mode,
        result: Result,
        paymentSelection: PaymentSelection?
    ) : PaymentSheetEvent(mode) {
        override val event: String = "payment_${analyticsValue(paymentSelection)}_$result"

        enum class Result(private val code: String) {
            Success("success"),
            Failure("failure");

            override fun toString(): String = code
        }
    }

    override fun toString(): String {
        return "mc_${mode}_$event"
    }

    internal companion object {
        const val PRODUCT_USAGE = "PaymentSheet"

        private fun analyticsValue(
            paymentSelection: PaymentSelection?
        ) = when (paymentSelection) {
            PaymentSelection.GooglePay -> "googlepay"
            is PaymentSelection.Saved -> "savedpm"
            is PaymentSelection.New -> "newpm"
            else -> "unknown"
        }
    }
}
