package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import javax.inject.Inject

@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CheckoutPresenter internal constructor(
    private val expressCheckoutElement: ExpressCheckoutElement,
) {

    fun paymentElement(): PaymentElement {
        TODO("Not yet implemented")
    }

    fun currencySelectorElement(): CurrencySelectorElement {
        TODO("Not yet implemented")
    }

    fun shippingAddressElement(): ShippingAddressElement {
        TODO("Not yet implemented")
    }

    fun expressCheckoutElement(): ExpressCheckoutElement {
        return expressCheckoutElement
    }

    fun confirm() {
        TODO("Not yet implemented")
    }

    class Factory @Inject internal constructor(
        private val expressCheckoutElementFactory: ExpressCheckoutElement.Factory,
    ) {
        fun create(
            controller: CheckoutController,
        ): CheckoutPresenter {
            return CheckoutPresenter(
                expressCheckoutElement = expressCheckoutElementFactory.create(
                    controllerState = controller.stateHolder.stateFlow,
                    confirmationState = controller.confirmationStateHolder.stateFlow,
                ),
            )
        }
    }
}
