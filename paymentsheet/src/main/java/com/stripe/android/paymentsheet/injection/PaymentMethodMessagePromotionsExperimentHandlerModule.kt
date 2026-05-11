package com.stripe.android.paymentsheet.injection

import com.stripe.android.common.analytics.experiment.DefaultPaymentMethodMessagePromotionsExperimentHandler
import com.stripe.android.common.analytics.experiment.PaymentMethodMessagePromotionsExperimentHandler
import dagger.Binds
import dagger.Module

@Module
internal interface PaymentMethodMessagePromotionsExperimentHandlerModule {
    @Binds
    fun bindsPaymentMethodMessageExperimentHandler(
        impl: DefaultPaymentMethodMessagePromotionsExperimentHandler
    ): PaymentMethodMessagePromotionsExperimentHandler
}
