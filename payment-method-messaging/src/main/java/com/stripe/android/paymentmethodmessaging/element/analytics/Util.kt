package com.stripe.android.paymentmethodmessaging.element.analytics

import androidx.compose.runtime.compositionLocalOf
import com.stripe.android.model.PaymentMethodMessage

internal val LocalElementTappedAnalyticsListener = compositionLocalOf<() -> Unit> {
    error("No ElementTappedAnalyticsListener Provided")
}

internal fun PaymentMethodMessage.paymentMethods() = when (this) {
    is PaymentMethodMessage.MultiPartner -> paymentMethods
    is PaymentMethodMessage.NoContent -> paymentMethods
    is PaymentMethodMessage.SinglePartner -> paymentMethods
    is PaymentMethodMessage.UnexpectedError -> emptyList()
}
