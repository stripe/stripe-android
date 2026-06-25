package com.stripe.android.payments.core.injection

import androidx.annotation.RestrictTo
import com.stripe.android.PaymentConfiguration
import com.stripe.android.StripeClient
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import dagger.Module
import dagger.Provides
import javax.inject.Named

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Module
class StripeClientModule {
    @Provides
    @Named(PUBLISHABLE_KEY)
    fun providePublishableKey(stripeClient: StripeClient): () -> String =
        { stripeClient.publishableKey }

    @Provides
    @Named(STRIPE_ACCOUNT_ID)
    fun provideStripeAccountId(stripeClient: StripeClient): () -> String? =
        { stripeClient.stripeAccountId }

    @Provides
    fun providePaymentConfiguration(stripeClient: StripeClient): PaymentConfiguration =
        PaymentConfiguration(
            publishableKey = stripeClient.publishableKey,
            stripeAccountId = stripeClient.stripeAccountId,
        )
}
