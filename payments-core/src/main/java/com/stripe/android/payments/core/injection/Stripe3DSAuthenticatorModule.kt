package com.stripe.android.payments.core.injection

import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.threeds2.Stripe3DS2Authenticator
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Singleton

/**
 * Provides [PaymentAuthenticator] for [NextActionData.SdkData.Use3DS2].
 * Requires 3ds2 SDK.
 */
@Module(
    includes = [
        Stripe3ds2TransactionModule::class
    ]
)
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class Stripe3DSAuthenticatorModule {
    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS2::class)
    abstract fun bindsStripe3DSAuthenticator(
        stripe3ds2Authenticator: Stripe3DS2Authenticator
    ): PaymentAuthenticator<StripeIntent>

    companion object {
        @Provides
        @Singleton
        fun providePaymentAuthConfig() = PaymentAuthConfig.get()
    }
}
