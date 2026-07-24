package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
enum class PaymentMethodLayout {
    Horizontal,
    Vertical,
}
