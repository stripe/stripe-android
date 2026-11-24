package com.stripe.android.paymentelement.embedded.linkinline

import app.cash.turbine.Turbine
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.linkinline.EmbeddedLinkInlineAuthController.ConfirmVerificationResult
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Suppress("TooManyFunctions")
internal class FakeEmbeddedLinkInlineAuth(
    override var isPresentingDialog: Boolean = false,
    var showsCard: Boolean = false,
    override var verifiesBeforeConfirming: Boolean = false,
    var verifyBeforeConfirmingResult: ConfirmVerificationResult = ConfirmVerificationResult.Unavailable,
    var linkRowOpensWallet: Boolean = false,
) : EmbeddedLinkInlineAuth {
    override var activityHost: EmbeddedLinkInlineActivityHost? = null
    val verifyBeforeConfirmingCalls = Turbine<Unit>()
    val onLinkRowSelectedCalls = Turbine<Unit>()
    val onWalletResultCalls = Turbine<LinkActivityResult>()
    val onConfiguredCalls = Turbine<OnConfiguredCall>()
    val onIntentConfirmedCalls = Turbine<Unit>()
    val registerDialogHostCalls = Turbine<() -> Boolean>()
    val closeDialogHostCalls = Turbine<Unit>()

    override val listOptions: StateFlow<EmbeddedLinkInlineListOptions> =
        MutableStateFlow(EmbeddedLinkInlineListOptions.Disabled)
    override val card: StateFlow<EmbeddedLinkInlineCardUi?> = stateFlowOf(null)
    override val dialog: StateFlow<EmbeddedLinkVerificationDialogSession?> = stateFlowOf(null)
    override val needsVisibility: StateFlow<Boolean> = stateFlowOf(false)

    override fun isUiPresentedOverElement(): Boolean = isPresentingDialog

    override suspend fun verifyBeforeConfirming(): ConfirmVerificationResult {
        verifyBeforeConfirmingCalls.add(Unit)
        return verifyBeforeConfirmingResult
    }

    override fun onLinkRowSelected(): Boolean {
        onLinkRowSelectedCalls.add(Unit)
        return linkRowOpensWallet
    }

    override fun onWalletResult(result: LinkActivityResult) {
        onWalletResultCalls.add(result)
    }

    override fun onConfigured(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ) {
        onConfiguredCalls.add(OnConfiguredCall(paymentMethodMetadata, customer, configuration))
    }

    override fun initialSelection(selection: PaymentSelection?): PaymentSelection? {
        return selection.takeUnless { showsCard && it is PaymentSelection.Link }
    }

    override fun isInExperiment(
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        configuration: EmbeddedPaymentElement.Configuration,
    ): Boolean = showsCard

    override fun onIntentConfirmed() {
        onIntentConfirmedCalls.add(Unit)
    }

    override fun onElementVisible() = Unit

    override fun onCodeTapped() = Unit

    override fun onClose() = Unit

    override fun onSwitchAccount() = Unit

    override fun onAction(action: EmbeddedLinkInlineCardState.Action) = Unit

    override fun registerDialogHost(isReady: () -> Boolean): AutoCloseable {
        registerDialogHostCalls.add(isReady)
        return AutoCloseable { closeDialogHostCalls.add(Unit) }
    }

    fun ensureAllEventsConsumed() {
        onConfiguredCalls.ensureAllEventsConsumed()
        onIntentConfirmedCalls.ensureAllEventsConsumed()
        registerDialogHostCalls.ensureAllEventsConsumed()
        closeDialogHostCalls.ensureAllEventsConsumed()
        verifyBeforeConfirmingCalls.ensureAllEventsConsumed()
        onLinkRowSelectedCalls.ensureAllEventsConsumed()
        onWalletResultCalls.ensureAllEventsConsumed()
    }

    data class OnConfiguredCall(
        val paymentMethodMetadata: PaymentMethodMetadata,
        val customer: CustomerState?,
        val configuration: EmbeddedPaymentElement.Configuration,
    )
}
