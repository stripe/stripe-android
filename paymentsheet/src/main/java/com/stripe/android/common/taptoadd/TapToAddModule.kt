package com.stripe.android.common.taptoadd

import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import dagger.Binds
import dagger.Module
import dagger.Provides

@OptIn(TapToAddPreview::class)
@Module(
    includes = [
        TapToAddConnectionModule::class,
    ]
)
internal interface TapToAddModule {
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
        fun providesTapToAddCollectionHandler(
            isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
            connectionManager: TapToAddConnectionManager,
            terminalWrapper: TerminalWrapper,
            tapToPayUxConfiguration: TapToPayUxConfiguration,
            errorReporter: ErrorReporter,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever
        ): TapToAddCollectionHandler {
            return TapToAddCollectionHandler.create(
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                connectionManager = connectionManager,
                terminalWrapper = terminalWrapper,
                tapToPayUxConfiguration = tapToPayUxConfiguration,
                errorReporter = errorReporter,
                createCardPresentSetupIntentCallbackRetriever = createCardPresentSetupIntentCallbackRetriever,
            )
        }
    }
}
