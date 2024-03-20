package com.stripe.android.paymentsheet

interface ExternalPaymentMethodsConfirmHandler {
    suspend fun onExternalPaymentMethodConfirm(
        externalPaymentMethodType: String,
        billingDetails: PaymentSheet.BillingDetails
    ): PaymentSheetResult
}