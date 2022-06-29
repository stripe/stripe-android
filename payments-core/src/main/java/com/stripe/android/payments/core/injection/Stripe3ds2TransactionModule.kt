package com.stripe.android.payments.core.injection

import android.content.Context
import com.stripe.android.core.injection.ENABLE_LOGGING
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.authentication.threeds2.DefaultStripe3ds2ChallengeResultProcessor
import com.stripe.android.payments.core.authentication.threeds2.Stripe3ds2ChallengeResultProcessor
import com.stripe.android.stripe3ds2.service.StripeThreeDs2Service
import com.stripe.android.stripe3ds2.service.StripeThreeDs2ServiceImpl
import com.stripe.android.stripe3ds2.transaction.MessageVersionRegistry
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Provides dependencies for 3ds2 transaction.
 */
@Module(
    subcomponents = [Stripe3ds2TransactionViewModelSubcomponent::class]
)
@SuppressWarnings("UnnecessaryAbstractClass")
internal abstract class Stripe3ds2TransactionModule {
    @Binds
    abstract fun bindsStripe3ds2ChallengeResultProcessor(
        defaultStripe3ds2ChallengeResultProcessor: DefaultStripe3ds2ChallengeResultProcessor
    ): Stripe3ds2ChallengeResultProcessor

    companion object {

        @Provides
        @Singleton
        fun provideMessageVersionRegistry() = MessageVersionRegistry()

        @Provides
        @Singleton
        fun provideStripeThreeDs2Service(
            context: Context,
            @Named(ENABLE_LOGGING) enableLogging: Boolean,
            @IOContext workContext: CoroutineContext
        ): StripeThreeDs2Service {
            return StripeThreeDs2ServiceImpl(context, enableLogging, workContext)
        }
    }
}
