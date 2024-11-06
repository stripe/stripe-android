package com.stripe.android.paymentsheet.ui

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod

internal interface UpdatePaymentMethodInteractor {
    val isLiveMode: Boolean
    val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod
    val card: PaymentMethod.Card
}

internal class DefaultUpdatePaymentMethodInteractor(
    override val isLiveMode: Boolean,
    override val displayableSavedPaymentMethod: DisplayableSavedPaymentMethod,
    override val card: PaymentMethod.Card,
) : UpdatePaymentMethodInteractor
