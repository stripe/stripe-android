package com.stripe.android.paymentsheet.wallet.embeddable

import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.injection.FormViewModelSubcomponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [
        FormViewModelSubcomponent::class
    ]
)
internal abstract class SavedPaymentMethodsViewModelModule {
    companion object {
        @Provides
        @Singleton
        fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Complete
    }
}