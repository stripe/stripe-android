package com.stripe.android.paymentmethodmessaging.view.injection

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        PaymentMethodMessagingModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        CoreCommonModule::class
    ]
)
internal interface PaymentMethodMessagingComponent {

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): PaymentMethodMessagingComponent
    }
}
