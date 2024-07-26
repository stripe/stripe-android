package com.stripe.android.paymentsheet.injection

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.IntentConfirmationHandler
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheetContractV2
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.paymentdatacollection.bacs.BacsMandateConfirmationLauncherFactory
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Module
internal class PaymentSheetViewModelModule(private val starterArgs: PaymentSheetContractV2.Args) {

    @Provides
    fun provideArgs(): PaymentSheetContractV2.Args {
        return starterArgs
    }

    @Provides
    fun providesIntentConfirmationHandlerFactory(
        application: Application,
        savedStateHandle: SavedStateHandle,
        paymentConfigurationProvider: Provider<PaymentConfiguration>,
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
        stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
        intentConfirmationInterceptor: IntentConfirmationInterceptor,
        errorReporter: ErrorReporter,
    ): IntentConfirmationHandler.Factory {
        return IntentConfirmationHandler.Factory(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentConfigurationProvider = paymentConfigurationProvider,
            stripePaymentLauncherAssistedFactory = stripePaymentLauncherAssistedFactory,
            bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
            application = application,
            statusBarColor = { starterArgs.statusBarColor },
            savedStateHandle = savedStateHandle,
            errorReporter = errorReporter,
        )
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
