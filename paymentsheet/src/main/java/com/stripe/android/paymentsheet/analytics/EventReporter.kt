package com.stripe.android.paymentsheet.analytics

import com.stripe.android.paymentsheet.PaymentSheetConfiguration
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EventReporter {
    fun onInit(configuration: PaymentSheetConfiguration?)

    fun onDismiss()

    fun onShowExistingPaymentOptions(
        linkEnabled: Boolean,
        activeLinkSession: Boolean,
        currency: String?
    )

    fun onShowNewPaymentOptionForm(
        linkEnabled: Boolean,
        activeLinkSession: Boolean,
        currency: String?
    )

    fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
        currency: String?
    )

    fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        currency: String?
    )

    fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        currency: String?
    )

    fun onLpmSpecFailure()

    fun onAutofill(type: String)

    enum class Mode(val code: String) {
        Complete("complete"),
        Custom("custom");

        override fun toString(): String = code
    }
}
