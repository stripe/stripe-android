package com.stripe.android.payments.core.injection

import com.stripe.android.ApiConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

/**
 * A [Module] that derives @Named(PUBLISHABLE_KEY) and @Named(STRIPE_ACCOUNT_ID)
 * from [ApiConfiguration.State].
 *
 * Include this module in components that provide [ApiConfiguration.State] via @BindsInstance
 * and need the @Named bindings for downstream consumers that haven't yet migrated to
 * Provider<ApiConfiguration.State>.
 */
@Module
internal object ApiConfigurationNamedModule {
    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        apiConfigurationState: Provider<ApiConfiguration.State>
    ): () -> String = { apiConfigurationState.get().publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        apiConfigurationState: Provider<ApiConfiguration.State>
    ): () -> String? = { apiConfigurationState.get().stripeAccountId }
}
