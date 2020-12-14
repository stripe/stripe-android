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

    class Payment(
        mode: EventReporter.Mode,
        private val result: Result,
        private val paymentSelection: PaymentSelection
    ) : PaymentSheetEvent(mode) {
        override val event: String
            get() {
                return when (paymentSelection) {
                    PaymentSelection.GooglePay -> "googlepay"
                    is PaymentSelection.Saved -> "savedpm"
                    is PaymentSelection.New -> "newpm"
                }.let { selectionValue ->
                    "payment_${selectionValue}_$result"
                }
            }

        enum class Result(private val code: String) {
            Success("success"),
            Failure("failure");

            override fun toString(): String = code
        }
    }

    override fun toString(): String {
        return "mc_${mode}_$event"
    }
}
