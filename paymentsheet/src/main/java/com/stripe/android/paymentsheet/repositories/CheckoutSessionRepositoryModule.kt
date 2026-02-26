package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Stripe
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal object CheckoutSessionRepositoryModule {
    @Provides
    fun provideCheckoutSessionRepository(
        logger: Logger,
        @IOContext workContext: CoroutineContext,
    ): CheckoutSessionRepository = DefaultCheckoutSessionRepository(
        stripeNetworkClient = DefaultStripeNetworkClient(
            logger = logger,
            workContext = workContext,
        ),
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
        appInfo = Stripe.appInfo,
    )
}
