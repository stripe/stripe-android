package com.stripe.android.paymentelement.embedded.content

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.verification.NoOpLinkInlineInteractor
import com.stripe.android.paymentelement.AnalyticEventCallback
import com.stripe.android.paymentelement.ExperimentalAnalyticEventCallbackApi
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.ui.DefaultWalletButtonsInteractor
import com.stripe.android.paymentsheet.ui.WalletButtonsInteractor
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Provider

internal fun interface EmbeddedWalletButtonsInteractorFactory {
    fun create(): WalletButtonsInteractor
}

@OptIn(ExperimentalAnalyticEventCallbackApi::class)
internal class DefaultEmbeddedWalletButtonsInteractorFactory @Inject constructor(
    private val embeddedLinkHelper: EmbeddedLinkHelper,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    private val errorReporter: ErrorReporter,
    private val eventReporter: EventReporter,
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkAccountHolder: LinkAccountHolder,
    private val analyticsCallbackProvider: Provider<AnalyticEventCallback?>,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedWalletButtonsInteractorFactory {

    override fun create(): WalletButtonsInteractor {
        return DefaultWalletButtonsInteractor.create(
            embeddedLinkHelper = embeddedLinkHelper,
            confirmationStateHolder = confirmationStateHolder,
            confirmationHandler = confirmationHandler,
            coroutineScope = coroutineScope,
            errorReporter = errorReporter,
            eventReporter = eventReporter,
            linkPaymentLauncher = linkPaymentLauncher,
            linkAccountHolder = linkAccountHolder,
            linkInlineInteractor = NoOpLinkInlineInteractor(),
            analyticsCallbackProvider = analyticsCallbackProvider,
        )
    }
}
