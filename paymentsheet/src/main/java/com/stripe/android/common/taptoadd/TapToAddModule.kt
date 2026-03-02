package com.stripe.android.common.taptoadd

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.networking.ApiRequest
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.CreateCardPresentSetupIntentCallback
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackReferences
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.PRODUCT_USAGE
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Provider

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
            stripeRepository: StripeRepository,
            terminalWrapper: TerminalWrapper,
            tapToPayUxConfiguration: TapToPayUxConfiguration,
            @Named(PRODUCT_USAGE) productUsage: Set<String>,
            paymentConfiguration: Provider<PaymentConfiguration>,
            requestOptions: ApiRequest.Options,
            errorReporter: ErrorReporter,
            createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever
        ): TapToAddCollectionHandler {
            return TapToAddCollectionHandler.create(
                isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
                connectionManager = connectionManager,
                terminalWrapper = terminalWrapper,
                stripeRepository = stripeRepository,
                productUsage = productUsage,
                paymentConfiguration = paymentConfiguration,
                tapToPayUxConfiguration = tapToPayUxConfiguration,
                errorReporter = errorReporter,
                createCardPresentSetupIntentCallbackRetriever = createCardPresentSetupIntentCallbackRetriever,
            )
        }
    }
}
