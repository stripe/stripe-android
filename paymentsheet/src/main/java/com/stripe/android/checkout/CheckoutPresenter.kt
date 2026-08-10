package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.elements.CurrencySelectorElement
import com.stripe.android.elements.PaymentElement
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
    private val checkoutConfirmationPerformerProvider: Provider<CheckoutConfirmationPerformer>,
) {

    /**
     * Returns the [PaymentElement], which displays and collects the customer's payment method.
     */
    fun paymentElement(): PaymentElement {
        return paymentElementProvider.get()
    }

    /**
     * Returns the [CurrencySelectorElement], which lets the customer choose the checkout currency
     * when multiple currencies are available.
     */
    fun currencySelectorElement(): CurrencySelectorElement {
        return currencySelectorElementProvider.get()
    }

    /**
     * Returns the [ShippingAddressElement], which collects the customer's shipping address.
     */
    fun shippingAddressElement(): ShippingAddressElement {
        return shippingAddressElementProvider.get()
    }

    /**
     * Returns the [ExpressCheckoutElement], which displays express payment buttons such as Google
     * Pay and Link.
     */
    fun expressCheckoutElement(): ExpressCheckoutElement {
        return expressCheckoutElementProvider.get()
    }

    /**
     * Confirms the payment for the customer's currently selected payment option.
     *
     * The result is delivered to the [CheckoutController.ResultCallback] supplied when building the
     * [CheckoutController].
     */
    fun confirm() {
        checkoutConfirmationPerformerProvider.get().confirm()
    }
}
