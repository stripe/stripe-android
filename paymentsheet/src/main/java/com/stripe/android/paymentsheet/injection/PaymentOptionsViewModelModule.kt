package com.stripe.android.paymentsheet.injection

import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
//import com.stripe.android.paymentelement.confirmation.STATUS_BAR_COLOR_PROVIDER
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        PaymentOptionsViewModelSubcomponent::class,
    ]
)
internal class PaymentOptionsViewModelModule {

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

//    @Provides
//    @Named(STATUS_BAR_COLOR_PROVIDER)
//    fun providesStatusBarColor(): () -> Int? {
//        return { null }
//    }

    @Provides
    @Named(ALLOWS_MANUAL_CONFIRMATION)
    fun provideAllowsManualConfirmation() = false
}
