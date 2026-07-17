package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.paymentelement.embedded.EmbeddedApiConfigurationHolder
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
internal object EmbeddedViewModelCredentialsModule {

    @Provides
    @Singleton
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        holder: EmbeddedApiConfigurationHolder,
    ): () -> String = {
        // Fallback to PaymentConfiguration is encapsulated inside the holder.
        holder.publishableKey
    }

    @Provides
    @Singleton
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        holder: EmbeddedApiConfigurationHolder,
    ): () -> String? = {
        // Fallback to PaymentConfiguration is encapsulated inside the holder.
        holder.stripeAccountId
    }
}
