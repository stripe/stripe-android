package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentsheet.ConfirmationHandler
import com.stripe.android.paymentsheet.DefaultConfirmationHandler
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@Module(includes = [PaymentSheetViewModelModule.ConfirmationHandlerModule::class])
internal class PaymentSheetViewModelModule(private val starterArgs: PaymentSheetContract.Args) {

    @Provides
    fun provideArgs(): PaymentSheetContract.Args {
        return starterArgs
    }

    @Provides
    fun providePrefsRepository(
        appContext: Context,
        @IOContext workContext: CoroutineContext
    ): PrefsRepository {
        return DefaultPrefsRepository(
            appContext,
            customerId = starterArgs.config?.customer?.id,
            workContext = workContext
        )
    }

    @Module
    interface ConfirmationHandlerModule {

        @Binds
        fun bindConfirmationHandler(impl: DefaultConfirmationHandler): ConfirmationHandler
    }
}
