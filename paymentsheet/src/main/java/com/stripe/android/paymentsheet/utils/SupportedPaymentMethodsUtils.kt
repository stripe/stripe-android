package com.stripe.android.paymentsheet.utils

import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.model.PaymentMethod

internal fun List<SupportedPaymentMethod>.isOnlyOneNonCardPaymentMethod() =
    this.size == 1 && this.first().code != PaymentMethod.Type.Card.code
