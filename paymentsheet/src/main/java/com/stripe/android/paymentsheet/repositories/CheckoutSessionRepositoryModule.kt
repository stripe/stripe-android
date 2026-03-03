package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.injection.StripeNetworkClientModule
import dagger.Binds
import dagger.Module

@Module(includes = [StripeNetworkClientModule::class])
internal interface CheckoutSessionRepositoryModule {
    @Binds
    fun bindCheckoutSessionRepository(
        impl: DefaultCheckoutSessionRepository,
    ): CheckoutSessionRepository
}
