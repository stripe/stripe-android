package com.stripe.android.customersheet.data.injection

import android.app.Application
import android.content.Context
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.core.injection.StripeNetworkClientModule
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module(includes = [StripeNetworkClientModule::class])
internal interface CustomerSheetDataSourceCommonModule {
    @Binds
    fun bindsElementsSessionRepository(
        elementsSessionRepository: RealElementsSessionRepository
    ): ElementsSessionRepository

    @Binds
    fun bindsApplicationContext(application: Application): Context

    companion object {
        @Provides
        fun provideApiConfigurationState(
            apiConfigurationProvider: () -> ApiConfiguration.State
        ): ApiConfiguration.State = apiConfigurationProvider()
    }
}
