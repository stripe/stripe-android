package com.stripe.android.paymentelement.confirmation.lpms

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.PUBLISHABLE_KEY
import com.stripe.android.core.injection.STRIPE_ACCOUNT_ID
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationInterceptor
import com.stripe.android.paymentelement.confirmation.intent.IntentConfirmationModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        StripeRepositoryModule::class,
        IntentConfirmationModule::class,
        LpmNetworkTestModule::class,
    ]
)
internal interface LpmNetworkTestComponent {
    val interceptor: IntentConfirmationInterceptor
    val client: StripeNetworkTestClient

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun publishableKeyProvider(
            @Named(PUBLISHABLE_KEY) publishableKeyProvider: () -> String,
        ): Builder

        @BindsInstance
        fun stripeAccountIdProvider(
            @Named(STRIPE_ACCOUNT_ID) stripeAccountIdProvider: () -> String?,
        ): Builder

        fun build(): LpmNetworkTestComponent
    }
}
