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
    private val expressCheckoutElementProvider: Provider<ExpressCheckoutElement>,
    // Injected eagerly (not a Provider) so its init registers the confirmation handler with the
    // activity as soon as the presenter is created.
    private val confirmationHelper: CheckoutConfirmationHelper,
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
        return expressCheckoutElementProvider.get()
    }

    fun confirm() {
        confirmationHelper.confirm()
    }
}
