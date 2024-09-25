package com.stripe.android.customersheet.data.injection

import android.app.Application
import android.content.Context
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.repositories.RealElementsSessionRepository
import dagger.Binds
import dagger.Module

@Module
internal interface CustomerSheetDataSourceCommonModule {
    @Binds
    fun bindsElementsSessionRepository(
        elementsSessionRepository: RealElementsSessionRepository
    ): ElementsSessionRepository

    @Binds
    fun bindsApplicationContext(application: Application): Context
}
