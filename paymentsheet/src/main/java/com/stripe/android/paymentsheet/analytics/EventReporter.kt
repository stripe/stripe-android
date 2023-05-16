package com.stripe.android.paymentsheet.analytics

import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection

internal interface EventReporter {

    fun onInit(
        configuration: PaymentSheet.Configuration?,
        isDecoupling: Boolean,
        isServerSideConfirmation: Boolean,
    )

    fun onDismiss(
        isDecoupling: Boolean,
    )

    fun onShowExistingPaymentOptions(
        linkEnabled: Boolean,
        activeLinkSession: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onShowNewPaymentOptionForm(
        linkEnabled: Boolean,
        activeLinkSession: Boolean,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onSelectPaymentOption(
        paymentSelection: PaymentSelection,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onPaymentSuccess(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onPaymentFailure(
        paymentSelection: PaymentSelection?,
        currency: String?,
        isDecoupling: Boolean,
    )

    fun onLpmSpecFailure(
        isDecoupling: Boolean,
    )

    fun onAutofill(
        type: String,
        isDecoupling: Boolean,
    )

    fun onForceSuccess()

    enum class Mode(val code: String) {
        Complete("complete"),
        Custom("custom");

        override fun toString(): String = code
    }
}
