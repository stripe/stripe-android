package com.stripe.android.paymentsheet.analytics

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EventReporter {

    fun onInit(
        configuration: PaymentSheet.Configuration?,
        isServerSideConfirmation: Boolean,
    )

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
