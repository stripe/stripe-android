package com.stripe.android.paymentsheet.repositories

import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.networking.StripeNetworkClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal interface CheckoutSessionRepositoryModule {
    @Binds
    fun bindCheckoutSessionRepository(
        impl: DefaultCheckoutSessionRepository,
    ): CheckoutSessionRepository

    companion object {
        @Provides
        fun provideStripeNetworkClient(
            logger: Logger,
            @IOContext workContext: CoroutineContext,
        ): StripeNetworkClient = DefaultStripeNetworkClient(
            logger = logger,
            workContext = workContext,
        )
    }
}
