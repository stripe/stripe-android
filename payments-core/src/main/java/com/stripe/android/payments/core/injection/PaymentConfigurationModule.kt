package com.stripe.android.payments.core.injection

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class PaymentConfigurationModule {
    @Provides
    fun providePaymentConfiguration(context: Context): PaymentConfiguration {
        return PaymentConfiguration.getInstance(context)
    }

    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> String = { paymentConfiguration.get().publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> String? = { paymentConfiguration.get().stripeAccountId }
}
