package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import com.stripe.android.paymentelement.confirmation.ALLOWS_MANUAL_CONFIRMATION
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module(
    subcomponents = [
        PaymentSheetViewModelSubcomponent::class,
    ]
)
internal abstract class PaymentSheetLauncherModule {
    @Binds
    abstract fun bindsApplicationForContext(application: Application): Context

    companion object {
        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Complete

        @Provides
        @Singleton
        @Named(PRODUCT_USAGE)
        fun provideProductUsageTokens() = setOf("PaymentSheet")

        @Provides
        @Singleton
        @Named(ALLOWS_MANUAL_CONFIRMATION)
        fun provideAllowsManualConfirmation() = false
    }
}
