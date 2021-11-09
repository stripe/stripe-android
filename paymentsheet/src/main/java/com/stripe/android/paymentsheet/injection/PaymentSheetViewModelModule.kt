package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule(private val starterArgs: PaymentSheetContract.Args) {

    @Provides
    fun provideArgs(): PaymentSheetContract.Args {
        return starterArgs
    }

    @Provides
    fun providePrefsRepository(
        appContext: Context,
        @com.stripe.android.core.injection.IOContext workContext: CoroutineContext
    ): PrefsRepository {
        return starterArgs.config?.customer?.let { (id) ->
            DefaultPrefsRepository(
                appContext,
                customerId = id,
                workContext = workContext
            )
        } ?: PrefsRepository.Noop()
    }
}
