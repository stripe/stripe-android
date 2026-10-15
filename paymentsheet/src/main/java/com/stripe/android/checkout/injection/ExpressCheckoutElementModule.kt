package com.stripe.android.checkout.injection

import com.stripe.android.checkout.CheckoutExpressCheckoutElementIntegration
import com.stripe.android.checkout.DefaultExpressCheckoutElementConfirmationPerformer
import com.stripe.android.elements.ece.ExpressCheckoutElementConfirmationPerformer
import com.stripe.android.elements.ece.ExpressCheckoutElementIntegration
import com.stripe.android.elements.ece.injection.ExpressCheckoutElementCommonModule
import dagger.Binds
import dagger.Module

@Module(includes = [ExpressCheckoutElementCommonModule::class])
internal interface ExpressCheckoutElementModule {
    @Binds
    fun bindExpressCheckoutElementIntegration(
        impl: CheckoutExpressCheckoutElementIntegration
    ): ExpressCheckoutElementIntegration

    @Binds
    fun bindExpressCheckoutElementConfirmationPerformer(
        impl: DefaultExpressCheckoutElementConfirmationPerformer
    ): ExpressCheckoutElementConfirmationPerformer
}
