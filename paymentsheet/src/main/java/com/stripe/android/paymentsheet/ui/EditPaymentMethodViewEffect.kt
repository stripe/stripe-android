package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod

internal sealed interface EditPaymentMethodViewEffect {
    data class OnRemoveRequested(
        val paymentMethod: PaymentMethod
    ) : EditPaymentMethodViewEffect

    data class OnUpdateRequested(
        val paymentMethod: PaymentMethod,
        val brand: CardBrand
    ) : EditPaymentMethodViewEffect
}
