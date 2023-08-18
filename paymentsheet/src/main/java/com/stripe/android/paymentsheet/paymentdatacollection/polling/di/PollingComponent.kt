package com.stripe.android.paymentsheet.paymentdatacollection.polling.di

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
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
        CoreCommonModule::class
    ]
)
internal interface PollingComponent {
    val subcomponentBuilder: PollingViewModelSubcomponent.Builder

    @Component.Builder
    interface Builder {

        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun config(config: IntentStatusPoller.Config): Builder

        @BindsInstance
        fun ioDispatcher(dispatcher: CoroutineDispatcher): Builder

        fun build(): PollingComponent
    }
}
