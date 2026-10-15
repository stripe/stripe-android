package com.stripe.android.elements.ece.injection

import com.stripe.android.elements.ece.DefaultExpressCheckoutElementInteractor
import com.stripe.android.elements.ece.ExpressCheckoutElementInteractor
import dagger.Binds
import dagger.Module

@Module
internal interface ExpressCheckoutElementInteractorModule {
    @Binds
    fun bindInteractor(impl: DefaultExpressCheckoutElementInteractor): ExpressCheckoutElementInteractor
}
