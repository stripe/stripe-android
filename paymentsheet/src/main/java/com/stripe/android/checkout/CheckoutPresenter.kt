package com.stripe.android.checkout

import androidx.activity.ComponentActivity
import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutPresenter internal constructor(
    private val activity: ComponentActivity,
    private val controller: CheckoutController,
) {

    fun paymentElement(): PaymentElement {
        return PaymentElement()
    }

    fun currencySelector(): CurrencySelector {
        return CurrencySelector()
    }

    fun shippingAddressElement(): ShippingAddressElement {
        return ShippingAddressElement()
    }

    fun confirm() {
        // TODO: Trigger confirmation flow, deliver result to controller's ResultCallback.
    }
}
