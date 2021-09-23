package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.payments.core.injection.IOContext
import com.stripe.android.payments.core.injection.Injector
import com.stripe.android.payments.core.injection.InjectorKey
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.analytics.EventReporter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module(
    subcomponents = [PaymentOptionsViewModelSubcomponent::class]
)
internal class PaymentOptionsViewModelModule {

    @Provides
    @Singleton
    fun provideEventReporterMode(): EventReporter.Mode = EventReporter.Mode.Custom

    @Provides
    @Singleton
    fun providePrefsRepositoryFactory(
        appContext: Context,
        @IOContext workContext: CoroutineContext
    ): (PaymentSheet.CustomerConfiguration?) -> PrefsRepository = { customerConfig ->
        customerConfig?.let {
            DefaultPrefsRepository(
                appContext,
                it.id,
                workContext
            )
        } ?: PrefsRepository.Noop()
    }

    /**
     * This module is only used when the app is recovered from a killed process,
     * where no [Injector] is available. Returns a dummy key instead.
     */
    @Provides
    @InjectorKey
    fun provideDummyInjectorKey(): Int = DUMMY_INJECTOR_KEY
}
