@file:OptIn(CheckoutSessionPreview::class)
package com.stripe.android.checkout.injection

import com.stripe.android.checkout.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import dagger.Binds


import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.Module

@Module
internal interface ExpressCheckoutElementModule {

    @Binds
    fun bindExpressCheckoutElementInteractor(
        impl: DefaultExpressCheckoutElementInteractor
    ): ExpressCheckoutElementInteractor
}
