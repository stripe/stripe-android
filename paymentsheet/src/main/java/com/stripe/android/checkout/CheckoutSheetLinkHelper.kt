package com.stripe.android.checkout

import androidx.activity.result.ActivityResultCaller
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.utils.determineFallbackPaymentSelectionAfterLinkLogout
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.paymentelement.EmbeddedPaymentElement
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.PaymentSelection.Link
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal const val PAYMENT_ELEMENT_LINK_LAUNCHER = "LinkPaymentLauncher_CheckoutPaymentElement"

internal interface CheckoutSheetLinkHelper {
    fun register(
        activityResultCaller: ActivityResultCaller,
        launchPaymentOptions: LaunchPaymentOptions,
    )

    fun unregister()

    fun launchLinkIfEligible(
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection?,
    ): Boolean

    fun interface LaunchPaymentOptions {
        fun launch(
            paymentMethodMetadata: PaymentMethodMetadata,
            customerState: CustomerState?,
            selection: PaymentSelection?,
            configuration: EmbeddedPaymentElement.Configuration,
        )
    }
}

@OptIn(CheckoutSessionPreview::class)
internal class DefaultCheckoutSheetLinkHelper @Inject constructor(
    private val selectionHolder: EmbeddedSelectionHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val errorReporter: ErrorReporter,
    private val sessionRefresher: CheckoutSessionRefresher,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val logger: Logger,
    private val checkoutControllerStateHolder: CheckoutControllerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val linkGateFactory: LinkGate.Factory,
    @Named(PAYMENT_ELEMENT_LINK_LAUNCHER) private val paymentElementLinkLauncher: LinkPaymentLauncher,
    private val taxRegionUpdater: SheetTaxRegionUpdater,
    @ViewModelScope private val coroutineScope: CoroutineScope,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
) : CheckoutSheetLinkHelper {
    override fun register(
        activityResultCaller: ActivityResultCaller,
        launchPaymentOptions: CheckoutSheetLinkHelper.LaunchPaymentOptions,
    ) {
        paymentElementLinkLauncher.register(
            activityResultCaller = activityResultCaller,
            callback = { result -> onLinkResult(result, launchPaymentOptions) },
        )
    }

    override fun unregister() {
        paymentElementLinkLauncher.unregister()
    }

    override fun launchLinkIfEligible(
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection?,
    ): Boolean {
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration
        val linkAccountInfo = linkAccountHolder.linkAccountInfo.value
        val shouldPresentLink = linkConfiguration != null &&
            !sheetStateHolder.declinedLink2FA &&
            selection is Link &&
            linkAccountInfo.account != null &&
            linkGateFactory.create(linkConfiguration).showRuxInFlowController

        if (!shouldPresentLink) return false

        paymentElementLinkLauncher.present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountInfo,
            launchMode = LinkLaunchMode.PaymentMethodSelection(selection.selectedPayment?.details),
            linkExpressMode = LinkExpressMode.ENABLED,
            statusBarColor = statusBarColor,
        )
        return true
    }

    private fun onLinkResult(
        result: LinkActivityResult,
        launchPaymentOptions: CheckoutSheetLinkHelper.LaunchPaymentOptions,
    ) {
        result.linkAccountUpdate?.let(::updateLinkAccount)

        when (result) {
            is LinkActivityResult.PaymentMethodObtained,
            is LinkActivityResult.Failed -> closeLinkPresentation()
            is LinkActivityResult.Canceled -> when (result.reason) {
                Reason.BackPressed -> {
                    val accountStatus = linkAccountHolder.linkAccountInfo.value.account?.accountStatus
                    if (accountStatus == AccountStatus.VerificationStarted) {
                        sheetStateHolder.declinedLink2FA = true
                    }
                    if ((selectionHolder.selection.value as? Link)?.selectedPayment == null) {
                        showCurrentPaymentOptions(launchPaymentOptions)
                    } else {
                        closeLinkPresentation()
                    }
                }
                Reason.LoggedOut -> {
                    updateLinkPaymentSelection(linkPaymentMethod = null, apply = true)
                    showCurrentPaymentOptions(launchPaymentOptions)
                }
                Reason.PayAnotherWay -> showCurrentPaymentOptions(launchPaymentOptions)
            }
            is LinkActivityResult.Completed -> handleCompletedLinkResult(
                linkPaymentMethod = result.selectedPayment,
                launchPaymentOptions = launchPaymentOptions,
            )
        }
    }

    private fun handleCompletedLinkResult(
        linkPaymentMethod: LinkPaymentMethod?,
        launchPaymentOptions: CheckoutSheetLinkHelper.LaunchPaymentOptions,
    ) {
        val newSelection = updateLinkPaymentSelection(linkPaymentMethod, apply = false)
        if (newSelection == null) {
            selectionHolder.setSelection(null)
            closeLinkPresentation()
            return
        }

        val state = checkoutControllerStateHolder.state
        val taxRegionUpdate = state?.let {
            taxRegionUpdater.prepareUpdate(it.paymentMethodMetadata, newSelection)
        }
        if (taxRegionUpdate == null) {
            selectionHolder.setSelection(newSelection)
            closeLinkPresentation()
            return
        }

        coroutineScope.launch {
            var taxRegionUpdated = false
            var paymentOptionsShown = false
            try {
                operationCoordinator.runMutation {
                    taxRegionUpdate().mapCatching { response ->
                        taxRegionUpdated = true
                        selectionHolder.setSelection(newSelection)
                        sessionRefresher.refresh(response)
                    }
                }.onFailure { error ->
                    if (taxRegionUpdated) {
                        logger.error("Failed to refresh the checkout session after Link selection.", error)
                    } else {
                        selectionHolder.setSelection(newSelection)
                        logger.error("Failed to update the tax region after Link selection.", error)
                        showCurrentPaymentOptions(launchPaymentOptions)
                        paymentOptionsShown = true
                    }
                }
            } finally {
                if (!paymentOptionsShown) {
                    closeLinkPresentation()
                }
            }
        }
    }

    private fun updateLinkPaymentSelection(
        linkPaymentMethod: LinkPaymentMethod?,
        apply: Boolean,
    ): PaymentSelection? {
        val currentSelection = selectionHolder.selection.value as? Link ?: return selectionHolder.selection.value
        val newSelection = if (linkPaymentMethod != null) {
            currentSelection.copy(selectedPayment = linkPaymentMethod)
        } else {
            val state = checkoutControllerStateHolder.state
            state?.let {
                determineFallbackPaymentSelectionAfterLinkLogout(
                    customer = customerStateHolder.customer.value,
                    paymentMethodMetadata = it.paymentMethodMetadata,
                )
            }
        }
        if (apply) {
            selectionHolder.setSelection(newSelection)
        }
        return newSelection
    }

    private fun updateLinkAccount(update: LinkAccountUpdate) {
        update.updateLinkAccount(linkAccountHolder)
        if (update !is LinkAccountUpdate.Value) return

        val state = checkoutControllerStateHolder.state ?: return
        val metadata = state.paymentMethodMetadata
        val accountStatus = update.account?.accountStatus ?: AccountStatus.SignedOut
        val updatedLinkState = when (val linkState = metadata.linkStateResult) {
            is LinkState -> linkState.copy(loginState = accountStatus.toLoginState())
            is LinkDisabledState, null -> linkState
        }
        checkoutControllerStateHolder.state = state.copy(
            paymentMethodMetadata = metadata.copy(linkStateResult = updatedLinkState),
        )
    }

    private fun showCurrentPaymentOptions(
        launchPaymentOptions: CheckoutSheetLinkHelper.LaunchPaymentOptions,
    ) {
        sheetStateHolder.sheetIsOpen = false
        val state = checkoutControllerStateHolder.state
        if (state == null) {
            errorReporter.report(
                ErrorReporter.UnexpectedErrorEvent.EMBEDDED_PRESENT_PAYMENT_OPTIONS_NOT_CONFIGURED
            )
            return
        }
        sheetStateHolder.sheetIsOpen = true
        launchPaymentOptions.launch(
            paymentMethodMetadata = state.paymentMethodMetadata,
            customerState = customerStateHolder.customer.value,
            selection = selectionHolder.selection.value,
            configuration = state.embeddedConfiguration,
        )
    }

    private fun closeLinkPresentation() {
        sheetStateHolder.sheetIsOpen = false
    }
}
