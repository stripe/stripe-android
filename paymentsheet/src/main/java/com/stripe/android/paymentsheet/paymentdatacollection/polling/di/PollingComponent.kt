package com.stripe.android.paymentsheet.paymentdatacollection.polling.di

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.networking.PaymentElementRequestSurfaceModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.polling.IntentStatusPoller
import dagger.BindsInstance
import dagger.Component
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        PollingViewModelModule::class,
        StripeRepositoryModule::class,
        PaymentElementRequestSurfaceModule::class,
        CoreCommonModule::class
    ]
)
internal interface PollingComponent {
    val subcomponentFactory: PollingViewModelSubcomponent.Factory

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
            @BindsInstance config: IntentStatusPoller.Config,
            @BindsInstance ioDispatcher: CoroutineDispatcher,
        ): PollingComponent
    }
}
