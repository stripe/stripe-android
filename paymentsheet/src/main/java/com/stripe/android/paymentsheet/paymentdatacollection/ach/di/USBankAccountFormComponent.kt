package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import android.app.Application
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        USBankAccountFormViewModelModule::class,
        CoreCommonModule::class,
        StripeRepositoryModule::class,
    ]
)
internal interface USBankAccountFormComponent {
    val subComponentBuilderProvider: Provider<USBankAccountFormViewModelSubcomponent.Builder>

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): USBankAccountFormComponent
    }
}
