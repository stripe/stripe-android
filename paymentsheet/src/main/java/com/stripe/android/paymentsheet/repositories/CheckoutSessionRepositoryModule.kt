package com.stripe.android.paymentsheet.repositories

import com.stripe.android.Stripe
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.core.networking.DefaultStripeNetworkClient
import com.stripe.android.core.version.StripeSdkVersion
import dagger.Module
import dagger.Provides
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module
internal object CheckoutSessionRepositoryModule {
    @Provides
    fun provideCheckoutSessionRepository(
        logger: Logger,
        @IOContext workContext: CoroutineContext,
        @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
    ): CheckoutSessionRepository = DefaultCheckoutSessionRepository(
        stripeNetworkClient = DefaultStripeNetworkClient(
            logger = logger,
            workContext = workContext,
        ),
        apiVersion = Stripe.API_VERSION,
        sdkVersion = StripeSdkVersion.VERSION,
        appInfo = Stripe.appInfo,
        publishableKeyProvider = publishableKeyProvider,
        stripeAccountIdProvider = stripeAccountIdProvider,
    )
}
