package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule(private val starterArgs: PaymentSheetContract.Args) {

    @Provides
    fun provideArgs(): PaymentSheetContract.Args {
        return starterArgs
    }

    @Provides
    @Named(STATUS_BAR_COLOR)
    fun providesStatusBarColor(): Int? {
        return starterArgs.statusBarColor
    }

    @Provides
    fun providePrefsRepository(
        appContext: Context,
        @IOContext workContext: CoroutineContext
    ): PrefsRepository {
        return DefaultPrefsRepository(
            appContext,
            customerId = starterArgs.config.customer?.id,
            workContext = workContext
        )
    }

    @Provides
    @Named(IS_LIVE_MODE)
    fun isLiveMode(
        paymentConfiguration: Provider<PaymentConfiguration>
    ): () -> Boolean = { paymentConfiguration.get().publishableKey.startsWith("pk_live") }
}
