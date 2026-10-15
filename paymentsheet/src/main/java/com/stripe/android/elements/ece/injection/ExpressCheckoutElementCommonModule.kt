package com.stripe.android.elements.ece.injection

import com.stripe.android.elements.ece.DefaultExpressCheckoutElementEventReporter
import com.stripe.android.elements.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.elements.ece.ExpressCheckoutElementEventReporter
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
import dagger.Binds
import dagger.Module

@Module
internal interface ExpressCheckoutElementCommonModule {
    @Binds
    fun bindInteractor(impl: DefaultExpressCheckoutElementInteractor): ExpressCheckoutElementInteractor

    @Binds
    fun bindEventReporter(impl: DefaultExpressCheckoutElementEventReporter): ExpressCheckoutElementEventReporter
}
