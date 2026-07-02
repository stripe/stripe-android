package com.stripe.android.paymentsheet.injection

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.PaymentSheetContract
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.ui.core.elements.autocomplete.PlacesClientProxy
import com.stripe.android.uicore.elements.DefaultIsPlacesAvailable
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule {

    @Provides
    @Named(STATUS_BAR_COLOR)
    fun providesStatusBarColor(starterArgs: PaymentSheetContract.Args): Int? {
        return starterArgs.statusBarColor
    }

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
    @ViewModelScope
    fun provideViewModelScope(): CoroutineScope {
        return CoroutineScope(Dispatchers.Main)
    }

    @Provides
    fun providePlacesClient(
        appContext: Context,
        starterArgs: PaymentSheetContract.Args,
        errorReporter: ErrorReporter,
    ): PlacesClientProxy? = createInlineAutocompletePlacesClient(
        context = appContext,
        googlePlacesApiKey = starterArgs.config.googlePlacesApiKey,
        errorReporter = errorReporter,
        isPlacesAvailable = { DefaultIsPlacesAvailable()() },
    )
}
