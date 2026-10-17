package com.stripe.android.paymentmethodmessaging.element.analytics

import com.stripe.android.model.PaymentMethodMessage

internal fun PaymentMethodMessage.paymentMethods() = when (this) {
    is PaymentMethodMessage.MultiPartner -> paymentMethods
    is PaymentMethodMessage.NoContent -> paymentMethods
    is PaymentMethodMessage.SinglePartner -> paymentMethods
    is PaymentMethodMessage.UnexpectedError -> emptyList()
}
