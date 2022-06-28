package com.stripe.android.paymentsheet.injection

import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.core.injection.Injector
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(
    subcomponents = [
        AddressElementViewModelSubcomponent::class,
        InputAddressViewModelSubcomponent::class,
        AutoCompleteViewModelSubcomponent::class,
        FormControllerSubcomponent::class
    ]
)
internal class AddressElementViewModelModule {
    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    /**
     * This module is only used when the app is recovered from a killed process,
     * where no [Injector] is available. Returns a dummy key instead.
     */
    @Provides
    @InjectorKey
    fun provideDummyInjectorKey(): String = DUMMY_INJECTOR_KEY
}
