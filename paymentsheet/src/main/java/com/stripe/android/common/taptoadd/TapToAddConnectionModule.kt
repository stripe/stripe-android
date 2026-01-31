package com.stripe.android.common.taptoadd

import android.content.Context
import android.os.Build
import com.stripe.android.PaymentConfiguration
import com.stripe.android.common.taptoadd.nfcdirect.DefaultIsNfcDirectAvailable
import com.stripe.android.common.taptoadd.nfcdirect.IsNfcDirectAvailable
import com.stripe.android.common.taptoadd.nfcdirect.NfcDirectConnectionManager
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Provider
import kotlin.coroutines.CoroutineContext

@Module
internal interface TapToAddConnectionModule {
    @Binds
    fun bindsStripeTerminalSdkAvailable(
        isStripeTerminalSdkAvailable: DefaultIsStripeTerminalSdkAvailable
    ): IsStripeTerminalSdkAvailable

    @Binds
    fun bindsStripeTerminalVersionValidator(
        stripeTerminalVersionValidator: DefaultStripeTerminalVersionValidator
    ): StripeTerminalVersionValidator

    @Binds
    fun bindsHasStripeTerminalCoreLibrary(
        hasStripeTerminalCoreLibrary: DefaultHasStripeTerminalCoreLibrary
    ): HasStripeTerminalCoreLibrary

    @Binds
    fun bindsHasStripeTerminalTapToPayLibrary(
        hasStripeTerminalTapToPayLibrary: DefaultHasStripeTerminalTapToPayLibrary
    ): HasStripeTerminalTapToPayLibrary

    @Binds
    fun bindsIsSimulatedProvider(
        isSimulatedProvider: DefaultTapToAddIsSimulatedProvider
    ): TapToAddIsSimulatedProvider

    @Binds
    fun bindsIsNfcDirectAvailable(
        isNfcDirectAvailable: DefaultIsNfcDirectAvailable
    ): IsNfcDirectAvailable

    @Binds
    fun bindsCreateCardPresentSetupIntentCallbackRetriever(
        callbackRetriever: DefaultCreateCardPresentSetupIntentCallbackRetriever
    ): CreateCardPresentSetupIntentCallbackRetriever

    companion object {
        @OptIn(TapToAddPreview::class)
        @Provides
        fun providesCreateCardPresentSetupIntentCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): CreateCardPresentSetupIntentCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.createCardPresentSetupIntentCallback
        }

        @Provides
        fun providesTerminalWrapper(): TerminalWrapper {
            return TerminalWrapper.create()
        }

        @Provides
        fun providesTapToAddConnectionManager(
            isNfcDirectAvailable: IsNfcDirectAvailable,
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            logger: Logger,
            applicationContext: Context,
            paymentConfiguration: Provider<PaymentConfiguration>,
            @IOContext workContext: CoroutineContext,
            callbackRetriever: CreateCardPresentSetupIntentCallbackRetriever,
            isSimulatedProvider: TapToAddIsSimulatedProvider,
        ): TapToAddConnectionManager {
            // Prefer NFC Direct (lightweight) over Terminal SDK when available
            if (isNfcDirectAvailable()) {
                return NfcDirectConnectionManager(
                    context = applicationContext,
                    workContext = workContext,
                )
            }

            return TapToAddConnectionManager.create(
                applicationContext = applicationContext,
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                paymentConfiguration = paymentConfiguration,
                isSimulatedProvider = isSimulatedProvider,
                logger = logger,
                callbackRetriever = callbackRetriever,
                workContext = workContext,
            )
        }
    }
}
