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

    init {
        controller.registerConfirmationHandler(activity)
    }

    fun paymentElement(): PaymentElement {
        return PaymentElement(controller)
    }

    fun currencySelector(): CurrencySelector {
        return CurrencySelector()
    }

    fun shippingAddressElement(): ShippingAddressElement {
        return ShippingAddressElement()
    }

    fun confirm() {
        controller.confirm()
    }
}
