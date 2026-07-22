@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import com.stripe.android.checkout.ece.DefaultExpressCheckoutElementEventReporter
import com.stripe.android.checkout.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.checkout.ece.ExpressCheckoutElementEventReporter
import com.stripe.android.checkout.ece.ExpressCheckoutElementInteractor
import com.stripe.android.paymentelement.CheckoutSessionPreview
import dagger.Binds
import dagger.Module

@Module
internal interface ExpressCheckoutElementModule {
    @Binds
    fun bindExpressCheckoutElementInteractor(
        impl: DefaultExpressCheckoutElementInteractor
    ): ExpressCheckoutElementInteractor

    @Binds
    fun bindExpressCheckoutElementEventReporter(
        impl: DefaultExpressCheckoutElementEventReporter
    ): ExpressCheckoutElementEventReporter
}
