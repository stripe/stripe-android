package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule {

    @Provides
    fun providePrefsRepository(
        appContext: Context,
        @IOContext workContext: CoroutineContext,
        starterArgs: PaymentSheetContract.Args,
    ): PrefsRepository {
        return DefaultPrefsRepository(
            appContext,
            customerId = starterArgs.config.customer?.id,
            workContext = workContext
        )
    }

    @Provides
    @PaymentElementCallbackIdentifier
    fun provideCallbackIdentifier(args: PaymentSheetContract.Args): String = args.paymentElementCallbackIdentifier

    @Provides
    @Singleton
    @ViewModelScope
    fun provideViewModelScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }
}
