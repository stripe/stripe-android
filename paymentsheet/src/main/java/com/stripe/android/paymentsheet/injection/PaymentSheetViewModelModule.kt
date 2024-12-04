package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContractV2
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Module
import dagger.Provides
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule(private val starterArgs: PaymentSheetContractV2.Args) {

    @Provides
    fun provideArgs(): PaymentSheetContractV2.Args {
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
}
