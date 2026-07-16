@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutPresenter
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.Subcomponent

@Subcomponent(
    modules = [
        CurrencySelectorElementModule::class,
        ExpressCheckoutElementModule::class,
    ]
)
internal interface CheckoutPresenterSubcomponent {
    val presenter: CheckoutPresenter

    @Subcomponent.Factory
    interface Factory {
        fun create(): CheckoutPresenterSubcomponent
    }
}
