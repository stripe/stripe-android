package com.stripe.android.common.taptoadd

import android.content.Context
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

        @OptIn(TapToAddPreview::class)
        @Provides
        fun providesTapToAddConnectionManager(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            logger: Logger,
            applicationContext: Context,
            @IOContext workContext: CoroutineContext,
            createCardPresentSetupIntentCallbackProvider: Provider<CreateCardPresentSetupIntentCallback?>,
            isSimulatedProvider: TapToAddIsSimulatedProvider,
        ): TapToAddConnectionManager {
            return TapToAddConnectionManager.create(
                applicationContext = applicationContext,
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                isSimulatedProvider = isSimulatedProvider,
                logger = logger,
                hasCreateCardPresentSetupIntentCallback = {
                    createCardPresentSetupIntentCallbackProvider.get() != null
                },
                workContext = workContext,
            )
        }
    }
}
