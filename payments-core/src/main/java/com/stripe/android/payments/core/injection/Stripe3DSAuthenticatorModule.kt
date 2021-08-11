package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.PaymentAuthConfig
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.StripeIntent.NextActionData
import com.stripe.android.payments.core.authentication.PaymentAuthenticator
import com.stripe.android.payments.core.authentication.threeds2.DefaultStripe3ds2ChallengeResultProcessor
import com.stripe.android.payments.core.authentication.threeds2.Stripe3DS2Authenticator
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2ChallengeResultProcessor
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Provides [PaymentAuthenticator] for [NextActionData.SdkData.Use3DS2].
 * Requires 3ds2 SDK.
 */
@Module
internal abstract class Stripe3DSAuthenticatorModule {
    @IntentAuthenticatorMap
    @Binds
    @IntoMap
    @IntentAuthenticatorKey(NextActionData.SdkData.Use3DS2::class)
    abstract fun bindsStripe3DSAuthenticator(
        stripe3ds2Authenticator: Stripe3DS2Authenticator
    ): PaymentAuthenticator<StripeIntent>

    @Binds
    abstract fun bindsStripe3ds2ChallengeResultProcessor(
        defaultStripe3ds2ChallengeResultProcessor: DefaultStripe3ds2ChallengeResultProcessor
    ): Stripe3ds2ChallengeResultProcessor

    companion object {
        @Provides
        @Singleton
        fun providePaymentAuthConfig() = PaymentAuthConfig.get()

        @Provides
        @Singleton
        fun provideMessageVersionRegistry() = MessageVersionRegistry()

        @Provides
        @Singleton
        fun provideStripeThreeDs2Service(
            context: Context,
            @Named(ENABLE_LOGGING) enableLogging: Boolean,
            @IOContext workContext: CoroutineContext,
        ): StripeThreeDs2Service {
            return StripeThreeDs2ServiceImpl(context, enableLogging, workContext)
        }
    }
}
