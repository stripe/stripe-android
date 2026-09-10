@file:OptIn(com.stripe.android.paymentelement.CheckoutSessionPreview::class)

package com.stripe.android.checkout

import androidx.activity.result.ActivityResultRegistry
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.checkout.injection.CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethodSelectionLauncher
import com.stripe.android.link.LinkPaymentMethodSelectionOutcome
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.handleLinkPaymentMethodSelectionResult
import com.stripe.android.paymentelement.embedded.content.DefaultEmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentOptionsPresenter
import com.stripe.android.paymentelement.embedded.content.SheetStateHolder
import com.stripe.android.paymentsheet.CustomerStateHolder
import javax.inject.Inject
import javax.inject.Named

internal class CheckoutLinkPaymentOptionsPresenter @Inject constructor(
    private val defaultPresenter: DefaultEmbeddedPaymentOptionsPresenter,
    private val selectionLauncher: LinkPaymentMethodSelectionLauncher,
    @Named(CHECKOUT_LINK_PAYMENT_METHOD_SELECTION_LAUNCHER)
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val activityResultRegistry: ActivityResultRegistry,
    private val lifecycleOwner: LifecycleOwner,
    private val stateHolder: CheckoutControllerStateHolder,
    private val customerStateHolder: CustomerStateHolder,
    private val linkAccountHolder: LinkAccountHolder,
    private val sheetStateHolder: SheetStateHolder,
    private val resultCallback: CheckoutController.ResultCallback,
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
                    sheetStateHolder.sheetIsOpen = false
                }
            }
        )
    }

    override fun present() {
        if (sheetStateHolder.sheetIsOpen) return
        if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.DESTROYED) {
            resultCallback.onResult(
                CheckoutController.Result.Failed(invalidHostStateError(lifecycleOwner))
            )
            return
        }
        val state = stateHolder.state
        if (state == null) {
            defaultPresenter.present()
            return
        }

        sheetStateHolder.sheetIsOpen = true
        val didLaunch = try {
            selectionLauncher.launchIfEligible(
                selection = state.paymentSelection,
                configuration = state.paymentMethodMetadata.linkState?.configuration,
                paymentMethodMetadata = state.paymentMethodMetadata,
                hasUserDeclinedVerification = state.linkEagerPresentationSuppressed,
            )
        } catch (error: IllegalStateException) {
            sheetStateHolder.sheetIsOpen = false
            resultCallback.onResult(
                CheckoutController.Result.Failed(invalidHostStateError(lifecycleOwner, error))
            )
            return
        }
        if (!didLaunch) {
            presentDefault()
        }
    }

    private fun onLinkResult(result: LinkActivityResult) {
        val state = stateHolder.state
        if (state == null) {
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
                if (outcome.showPaymentOptions) {
                    presentDefault()
                } else {
                    sheetStateHolder.sheetIsOpen = false
                }
            }
            is LinkPaymentMethodSelectionOutcome.UpdatedLinkMetadata -> updateLinkMetadata(outcome)
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
