package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.effectiveLinkBrand
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.embedded.EmbeddedLaunchMode
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

internal interface SheetLinkPaymentMethodSelectionHandler {
    fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    )

    fun launchIfEligible(selection: PaymentSelection?): Boolean
}

@Singleton
internal class DefaultSheetLinkPaymentMethodSelectionHandler @Inject constructor(
    private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkGateFactory: LinkGate.Factory,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val launchMode: EmbeddedLaunchMode,
    private val linkAccountHolder: LinkAccountHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val stateHolder: SheetActivityStateHolder,
    private val continueCoordinatorProvider: Provider<SheetActivityContinueCoordinator>,
) : SheetLinkPaymentMethodSelectionHandler {

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        if (launchMode !is EmbeddedLaunchMode.PaymentOptions) return

        linkPaymentLauncher.register(activityResultCaller, ::onLinkResult)
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkPaymentLauncher.unregister()
                }
            }
        )
    }

    override fun launchIfEligible(selection: PaymentSelection?): Boolean {
        if (launchMode !is EmbeddedLaunchMode.PaymentOptions || selection !is PaymentSelection.Link) return false
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return false
        if (!linkGateFactory.create(linkConfiguration).showRuxInFlowController) return false

        stateHolder.updateError(null)
        linkPaymentLauncher.present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
            launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null),
            linkExpressMode = LinkExpressMode.ENABLED,
            // Selection-only launch; Link returns a selection here and never confirms, so
            // there is no auth surface to color.
            statusBarColor = null,
        )
        return true
    }

    private fun onLinkResult(result: LinkActivityResult) {
        result.linkAccountUpdate?.updateLinkAccount(linkAccountHolder)
        when (result) {
            is LinkActivityResult.Canceled -> Unit
            is LinkActivityResult.Failed -> stateHolder.updateError(result.error.stripeErrorMessage())
            is LinkActivityResult.Completed -> onLinkCompleted(result)
            is LinkActivityResult.PaymentMethodObtained -> {
                val error = IllegalStateException(
                    "PaymentMethodObtained is not expected from authentication only Link flows"
                )
                stateHolder.updateError(error.stripeErrorMessage())
            }
        }
    }

    private fun onLinkCompleted(result: LinkActivityResult.Completed) {
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return
        selectionHolder.setSelection(
            PaymentSelection.Link(
                brand = linkConfiguration.effectiveLinkBrand(linkAccountHolder.linkAccountInfo.value.account),
                selectedPayment = result.selectedPayment,
                shippingAddress = result.shippingAddress,
            )
        )
        continueCoordinatorProvider.get().completeSelection()
    }
}
