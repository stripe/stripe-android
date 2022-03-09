package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.core.injection.CoroutineContextModule
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
        USBankAccountFormModule::class,
        StripeRepositoryModule::class,
        LoggingModule::class
    ]
)
internal interface USBankAccountFormComponent {
    val viewModel: USBankAccountFormViewModel

    fun inject(factory: USBankAccountFormViewModel.Factory)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        @BindsInstance
        fun configuration(configuration: USBankAccountFormViewModel.Args): Builder

        @BindsInstance
        fun savedStateHandle(savedStateHandle: SavedStateHandle): Builder

        fun build(): USBankAccountFormComponent
    }
}
