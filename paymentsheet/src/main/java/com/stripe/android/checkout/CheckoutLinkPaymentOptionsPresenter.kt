@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.injection.CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethodSelectionLauncher
import com.stripe.android.link.LinkPaymentMethodSelectionOutcome
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.handleLinkPaymentMethodSelectionResult
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentelement.embedded.sheet.SheetTaxRegionUpdater
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

internal class CheckoutLinkPaymentOptionsPresenter @Inject constructor(
    private val defaultPresenter: DefaultEmbeddedPaymentOptionsPresenter,
    private val selectionLauncher: LinkPaymentMethodSelectionLauncher,
    @Named(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER)
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val activityResultRegistry: ActivityResultRegistry,
    private val lifecycleOwner: LifecycleOwner,
    private val stateHolder: CheckoutElementsControllerStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val taxRegionUpdater: SheetTaxRegionUpdater,
    private val operationCoordinator: CheckoutOperationCoordinator,
    private val sessionRefresher: CheckoutSessionRefresher,
    private val logger: Logger,
    @ViewModelScope private val coroutineScope: CoroutineScope,
) : EmbeddedPaymentOptionsPresenter {
    init {
        linkPaymentLauncher.register(
            key = CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER,
            activityResultRegistry = activityResultRegistry,
            callback = ::onLinkResult,
        )
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkPaymentLauncher.unregister()
                }
            }
        )
    }

    override fun present() {
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) return
        if (sheetStateHolder.sheetIsOpen) return
        val state = stateHolder.state
        if (state == null || state.paymentMethodMetadata == null) {
            defaultPresenter.present()
            return
        }

        sheetStateHolder.sheetIsOpen = true
        val didLaunch = selectionLauncher.launchIfEligible(
            selection = state.paymentSelection,
            configuration = state.paymentMethodMetadata.linkState?.configuration,
            paymentMethodMetadata = state.paymentMethodMetadata,
            hasUserDeclinedVerification = state.linkEagerPresentationSuppressed,
        )
        if (!didLaunch) {
            presentDefault()
        }
    }

    private fun onLinkResult(result: LinkActivityResult) {
        val state = stateHolder.state
        if (state == null || state.paymentMethodMetadata == null) {
            sheetStateHolder.sheetIsOpen = false
            return
        }
        handleLinkPaymentMethodSelectionResult(
            result = result,
            selection = state.paymentSelection,
            customerState = customerStateHolder.customer.value,
            paymentMethodMetadata = state.paymentMethodMetadata,
            currentLinkAccountInfo = linkAccountHolder.linkAccountInfo.value,
        ).forEach(::applyOutcome)
    }

    private fun applyOutcome(outcome: LinkPaymentMethodSelectionOutcome) {
        when (outcome) {
            LinkPaymentMethodSelectionOutcome.Dismiss -> sheetStateHolder.sheetIsOpen = false
            LinkPaymentMethodSelectionOutcome.ShowPaymentOptions -> presentDefault()
            LinkPaymentMethodSelectionOutcome.SuppressFutureEagerPresentation -> {
                stateHolder.state = stateHolder.state?.copy(linkEagerPresentationSuppressed = true)
            }
            is LinkPaymentMethodSelectionOutcome.UpdateSelection -> {
                stateHolder.setSelection(outcome.selection)
                syncTaxRegionForSelection(outcome.selection)
                if (outcome.showPaymentOptions) {
                    presentDefault()
                } else {
                    sheetStateHolder.sheetIsOpen = false
                }
            }
            is LinkPaymentMethodSelectionOutcome.UpdatedLinkMetadata -> updateLinkMetadata(outcome)
        }
    }

    /**
     * Native Link doesn't show the total when selecting a payment method, so sync the selected payment method's
     * billing address to tax calculation once it's selected.
     */
    private fun syncTaxRegionForSelection(selection: PaymentSelection?) {
        if (selection !is PaymentSelection.Link) return
        val taxRegionUpdate = taxRegionUpdater.prepareUpdate(
            checkoutSessionResponse = stateHolder.state?.checkoutSessionResponse,
            selection = selection,
        ) ?: return
        coroutineScope.launch {
            operationCoordinator.runMutation {
                taxRegionUpdate().mapCatching { response -> sessionRefresher.refresh(response) }
            }.onFailure {
                logger.error("Failed to update the tax region for the selected Link payment method.", it)
            }
        }
    }

    private fun updateLinkMetadata(outcome: LinkPaymentMethodSelectionOutcome.UpdatedLinkMetadata) {
        linkAccountHolder.set(outcome.linkAccountInfo)
        stateHolder.state = stateHolder.state?.copy(
            paymentMethodMetadata = outcome.paymentMethodMetadata,
        )
    }

    private fun presentDefault() {
        sheetStateHolder.sheetIsOpen = false
        defaultPresenter.present()
    }
}
