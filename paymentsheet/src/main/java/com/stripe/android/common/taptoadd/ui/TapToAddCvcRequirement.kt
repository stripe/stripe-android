package com.stripe.android.common.taptoadd.ui

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod

internal fun requiresTapToAddCvcCollection(
    paymentMethodMetadata: PaymentMethodMetadata,
    paymentMethod: PaymentMethod,
): Boolean {
    return paymentMethod.type == PaymentMethod.Type.Card &&
        (paymentMethodMetadata.stripeIntent as? PaymentIntent)?.requireCvcRecollection == true
}
