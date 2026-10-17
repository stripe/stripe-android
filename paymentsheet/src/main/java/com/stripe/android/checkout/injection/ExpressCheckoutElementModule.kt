@file:OptIn(CheckoutSessionPreview::class)

package com.stripe.android.checkout.injection

import com.stripe.android.elements.ece.DefaultExpressCheckoutElementConfirmationPerformer
import com.stripe.android.elements.ece.DefaultExpressCheckoutElementEventReporter
import com.stripe.android.elements.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.elements.ece.ExpressCheckoutElementConfirmationPerformer
import com.stripe.android.elements.ece.ExpressCheckoutElementEventReporter
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
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
    fun bindExpressCheckoutElementConfirmationPerformer(
        impl: DefaultExpressCheckoutElementConfirmationPerformer
    ): ExpressCheckoutElementConfirmationPerformer

    @Binds
    fun bindExpressCheckoutElementEventReporter(
        impl: DefaultExpressCheckoutElementEventReporter
    ): ExpressCheckoutElementEventReporter
}
