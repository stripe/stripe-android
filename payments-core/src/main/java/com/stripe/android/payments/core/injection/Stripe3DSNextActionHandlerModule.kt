package com.stripe.android.payments.core.injection

import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.PaymentNextActionHandler
import com.stripe.android.payments.core.authentication.threeds2.Stripe3DS2NextActionHandler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Singleton

/**
 * Provides [PaymentNextActionHandler] for [NextActionData.SdkData.Use3DS2].
 * Requires 3ds2 SDK.
 */
@Module(
    includes = [
        Stripe3ds2TransactionModule::class
    ]
)
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class Stripe3DSNextActionHandlerModule {
    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS2::class)
    abstract fun bindsStripe3DSNextActionHandler(
        stripe3ds2NextActionHandler: Stripe3DS2NextActionHandler
    ): PaymentNextActionHandler<StripeIntent>

    companion object {
        @Provides
        @Singleton
        fun providePaymentAuthConfig() = PaymentAuthConfig.get()
    }
}
