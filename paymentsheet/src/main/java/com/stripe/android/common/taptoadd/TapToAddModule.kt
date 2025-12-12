package com.stripe.android.common.taptoadd

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.BuildConfig
import dagger.Binds
import dagger.Module
import dagger.Provides
import kotlin.coroutines.CoroutineContext

@OptIn(TapToAddPreview::class)
@Module
internal interface TapToAddModule {
    @Binds
    fun bindsStripeTerminalSdkAvailable(
        isStripeTerminalSdkAvailable: DefaultIsStripeTerminalSdkAvailable
    ): IsStripeTerminalSdkAvailable

    @Binds
    fun bindsTerminalWrapper(
        terminalWrapper: DefaultTerminalWrapper
    ): TerminalWrapper

    @Binds
    fun bindsCreateCardPresentSetupIntentCallbackRetriever(
        retriever: DefaultCreateCardPresentSetupIntentCallbackRetriever
    ): CreateCardPresentSetupIntentCallbackRetriever

    companion object {
        @Provides
        fun providesCreateCardPresentSetupIntentCallback(
            @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
        ): CreateCardPresentSetupIntentCallback? {
            return PaymentElementCallbackReferences[paymentElementCallbackIdentifier]
                ?.createCardPresentSetupIntentCallback
        }

        @Provides
        fun providesTapToAddConnectionManager(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            terminalWrapper: TerminalWrapper,
            errorReporter: ErrorReporter,
            applicationContext: Context,
            @IOContext workContext: CoroutineContext
        ): TapToAddConnectionManager {
            return TapToAddConnectionManager.create(
                applicationContext = applicationContext,
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                terminalWrapper = terminalWrapper,
                errorReporter = errorReporter,
                isSimulated = BuildConfig.DEBUG,
                workContext = workContext,
            )
        }

        @Provides
        fun providesTapToAddCollectionHandler(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            connectionManager: TapToAddConnectionManager,
            terminalWrapper: TerminalWrapper,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever
        ): TapToAddCollectionHandler {
            return TapToAddCollectionHandler.create(
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                connectionManager = connectionManager,
                terminalWrapper = terminalWrapper,
                createCardPresentSetupIntentCallbackRetriever = createCardPresentSetupIntentCallbackRetriever,
            )
        }
    }
}
