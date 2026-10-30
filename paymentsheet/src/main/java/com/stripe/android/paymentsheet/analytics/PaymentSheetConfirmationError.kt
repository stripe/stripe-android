package com.stripe.android.paymentsheet.analytics

internal data class PaymentSheetConfirmationError(
    val cause: Throwable,
    val analyticsValue: String? = null,
    val errorCode: String? = null,
)
