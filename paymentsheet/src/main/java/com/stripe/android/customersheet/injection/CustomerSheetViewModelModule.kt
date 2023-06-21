package com.stripe.android.customersheet.injection

import android.app.Application
import android.content.Context
import android.content.res.Resources
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import com.stripe.android.ui.core.forms.resources.LpmRepository
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [
        FormViewModelSubcomponent::class,
    ]
)
internal class CustomerSheetViewModelModule {

    @Provides
    fun paymentConfiguration(application: Application): PaymentConfiguration {
        return PaymentConfiguration.getInstance(application)
    }

    @Provides
    fun resources(application: Application): Resources {
        return application.resources
    }

    @Provides
    fun context(application: Application): Context {
        return application
    }

    @Provides
    @IOContext
    fun ioContext(): CoroutineContext {
        return Dispatchers.IO
    }

    @Provides
    fun provideLpmRepository(resources: Resources): LpmRepository {
        return LpmRepository.getInstance(
            LpmRepository.LpmRepositoryArguments(resources)
        )
    }
}
