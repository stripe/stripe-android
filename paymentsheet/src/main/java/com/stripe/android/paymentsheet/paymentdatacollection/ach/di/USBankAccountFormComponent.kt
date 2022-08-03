package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import android.app.Application
import com.stripe.android.core.injection.CoroutineContextModule
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.core.injection.LoggingModule
import com.stripe.android.payments.core.injection.StripeRepositoryModule
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormViewModel
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        CoroutineContextModule::class,
        USBankAccountFormViewModelModule::class,
        StripeRepositoryModule::class,
        LoggingModule::class
    ]
)
internal interface USBankAccountFormComponent {
    fun inject(factory: USBankAccountFormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun injectorKey(@InjectorKey injectorKey: String): Builder

        fun build(): USBankAccountFormComponent
    }
}
