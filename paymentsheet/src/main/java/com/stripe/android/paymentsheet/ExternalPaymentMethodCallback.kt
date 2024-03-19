package com.stripe.android.paymentsheet

typealias ExternalPaymentMethodConfirmHandler = (
    externalPaymentMethodType: String, billingDetails: PaymentSheet.BillingDetails?, completion: (PaymentSheetResult) -> Unit
) -> Unit
class ExternalPaymentMethodCallback {
    companion object {
        var callback: ExternalPaymentMethodConfirmHandler? = null
    }
}