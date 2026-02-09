package com.stripe.android.paymentsheet.paymentdatacollection.ach.di

import android.app.Application
import com.stripe.android.core.injection.ApplicationContextModule
import com.stripe.android.core.injection.CoreCommonModule
import com.stripe.android.core.injection.CoroutineContextModule
import dagger.BindsInstance
import dagger.Component
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ApplicationContextModule::class,
        CoroutineContextModule::class,
        USBankAccountFormViewModelModule::class,
        CoreCommonModule::class
    ]
)
internal interface USBankAccountFormComponent {
    val subComponentFactoryProvider: Provider<USBankAccountFormViewModelSubcomponent.Factory>

    @Component.Factory
    interface Factory {
        fun create(
            @BindsInstance application: Application,
        ): USBankAccountFormComponent
    }
}
