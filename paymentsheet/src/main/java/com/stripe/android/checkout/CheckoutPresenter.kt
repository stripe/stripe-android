package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutPresenter internal constructor() {

    fun paymentElement(): PaymentElement {
        TODO("Not yet implemented")
    }

    fun currencySelectorElement(): CurrencySelectorElement {
        TODO("Not yet implemented")
    }

    fun shippingAddressElement(): ShippingAddressElement {
        TODO("Not yet implemented")
    }

    fun confirm() {
        TODO("Not yet implemented")
    }
}
