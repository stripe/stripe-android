package com.stripe.android.paymentsheet.injection

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.googlepaylauncher.injection.GooglePayPaymentMethodLauncherFactory
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.paymentlauncher.StripePaymentLauncherAssistedFactory
import com.stripe.android.paymentsheet.DefaultPrefsRepository
import com.stripe.android.paymentsheet.IntentConfirmationHandler
import com.stripe.android.paymentsheet.IntentConfirmationInterceptor
import com.stripe.android.paymentsheet.PaymentSheetContractV2
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.paymentdatacollection.ach.CollectBankAccountFlowLauncherFactory
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
        savedStateHandle: SavedStateHandle,
        paymentConfigurationProvider: Provider<PaymentConfiguration>,
        bacsMandateConfirmationLauncherFactory: BacsMandateConfirmationLauncherFactory,
        googlePayPaymentMethodLauncherFactory: GooglePayPaymentMethodLauncherFactory,
        stripePaymentLauncherAssistedFactory: StripePaymentLauncherAssistedFactory,
        collectBankAccountFlowLauncherFactory: CollectBankAccountFlowLauncherFactory,
        intentConfirmationInterceptor: IntentConfirmationInterceptor,
        errorReporter: ErrorReporter,
        logger: UserFacingLogger,
    ): IntentConfirmationHandler.Factory {
        return IntentConfirmationHandler.Factory(
            intentConfirmationInterceptor = intentConfirmationInterceptor,
            paymentConfigurationProvider = paymentConfigurationProvider,
            stripePaymentLauncherAssistedFactory = stripePaymentLauncherAssistedFactory,
            bacsMandateConfirmationLauncherFactory = bacsMandateConfirmationLauncherFactory,
            googlePayPaymentMethodLauncherFactory = googlePayPaymentMethodLauncherFactory,
            statusBarColor = { starterArgs.statusBarColor },
            savedStateHandle = savedStateHandle,
            errorReporter = errorReporter,
            logger = logger,
            collectBankAccountFlowLauncherFactory = collectBankAccountFlowLauncherFactory,
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
