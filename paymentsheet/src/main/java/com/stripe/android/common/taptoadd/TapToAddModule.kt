package com.stripe.android.common.taptoadd

import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.utils.UserFacingLogger
import com.stripe.android.networking.StripeRepository
import com.stripe.android.paymentelement.TapToAddPreview
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.stripeterminal.external.models.TapToPayUxConfiguration
import dagger.Module
import dagger.Provides

@OptIn(TapToAddPreview::class)
@Module(
    includes = [
        TapToAddConnectionModule::class,
    ]
)
internal class TapToAddModule {
    @Provides
    fun providesTapToAddCollectionHandler(
        isStripeTerminalSdkAvailable: IsStripeTerminalSdkAvailable,
        connectionManager: TapToAddConnectionManager,
        stripeRepository: StripeRepository,
        paymentConfiguration: PaymentConfiguration,
        terminalWrapper: TerminalWrapper,
        tapToPayUxConfiguration: TapToPayUxConfiguration,
        userFacingLogger: UserFacingLogger,
        errorReporter: ErrorReporter,
        createCardPresentSetupIntentCallbackRetriever: CreateCardPresentSetupIntentCallbackRetriever
    ): TapToAddCollectionHandler {
        return TapToAddCollectionHandler.create(
            isStripeTerminalSdkAvailable = isStripeTerminalSdkAvailable,
            connectionManager = connectionManager,
            terminalWrapper = terminalWrapper,
            stripeRepository = stripeRepository,
            paymentConfiguration = paymentConfiguration,
            tapToPayUxConfiguration = tapToPayUxConfiguration,
            errorReporter = errorReporter,
            userFacingLogger = userFacingLogger,
            createCardPresentSetupIntentCallbackRetriever = createCardPresentSetupIntentCallbackRetriever,
        )
    }
}
