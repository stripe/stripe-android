package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContractV2
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module
internal interface PaymentSheetViewModelModule {

    companion object {
        @Provides
        fun providePrefsRepository(
            starterArgs: PaymentSheetContractV2.Args,
            appContext: Context,
            @IOContext workContext: CoroutineContext
        ): PrefsRepository {
            return DefaultPrefsRepository(
                appContext,
                customerId = starterArgs.config?.customer?.id,
                workContext = workContext
            )
        }
    }
}
