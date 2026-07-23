package com.stripe.android.paymentsheet.injection

import com.stripe.android.ApiConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import dagger.Module
import dagger.Provides
import javax.inject.Named

/**
 * Provides [ApiConfiguration.State] from [PaymentMethodMetadata].
 */
@Module
internal object ApiConfigurationFromMetadataModule {
    @Provides
    fun providesApiConfiguration(
        paymentMethodMetadata: PaymentMethodMetadata
    ): ApiConfiguration.State = paymentMethodMetadata.apiConfiguration
}

/**
 * Provides [Named] PUBLISHABLE_KEY and STRIPE_ACCOUNT_ID bindings derived from
 * [ApiConfiguration.State]. Use this in components that need to supply these bindings to
 * payments-core consumers (e.g. [com.stripe.android.networking.StripeApiRepository]) while
 * sourcing credentials from ApiConfiguration rather than PaymentConfiguration directly.
 */
@Module
internal object NamedKeysFromApiConfigModule {
    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providesPublishableKeyProvider(
        apiConfiguration: ApiConfiguration.State
    ): () -> String = { apiConfiguration.publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun providesStripeAccountIdProvider(
        apiConfiguration: ApiConfiguration.State
    ): () -> String? = { apiConfiguration.stripeAccountId }
}
