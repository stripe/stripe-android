package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject
import javax.inject.Provider

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutPresenter @Inject internal constructor(
    private val paymentElementProvider: Provider<PaymentElement>,
    private val currencySelectorElementProvider: Provider<CurrencySelectorElement>,
    private val shippingAddressElementProvider: Provider<ShippingAddressElement>,
    private val expressCheckoutElement: ExpressCheckoutElement,
) {

    fun paymentElement(): PaymentElement {
        return paymentElementProvider.get()
    }

    fun currencySelectorElement(): CurrencySelectorElement {
        return currencySelectorElementProvider.get()
    }

    fun shippingAddressElement(): ShippingAddressElement {
        return shippingAddressElementProvider.get()
    }

    fun expressCheckoutElement(): ExpressCheckoutElement {
        return expressCheckoutElement
    }

    fun confirm() {
        TODO("Not yet implemented")
    }
}
