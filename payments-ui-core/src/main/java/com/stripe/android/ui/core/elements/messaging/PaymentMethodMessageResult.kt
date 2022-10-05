package com.stripe.android.ui.core.elements.messaging

sealed class PaymentMethodMessageResult {
    object Loading : PaymentMethodMessageResult()
    class Success(
        val data: PaymentMethodMessageData
    ) : PaymentMethodMessageResult()
    class Failure(
        val error: Throwable
    ) : PaymentMethodMessageResult()
}
