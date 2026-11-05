package com.stripe.android.paymentelement.embedded.sheet

import androidx.activity.result.ActivityResultCaller
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.link.LinkActivityContract
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.LinkStore
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.effectiveLinkBrand
import com.stripe.android.link.injection.LinkAnalyticsComponent
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.callbacks.PaymentElementCallbackIdentifier
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

internal interface SheetLinkPaymentMethodLauncher {
    fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    )

    fun present(linkConfiguration: LinkConfiguration)
}

/**
 * Opens the Link payment method picker from FlowController's payment options sheet, and returns the chosen Link
 * payment method as the selection.
 */
@Singleton
internal class FlowControllerSheetLinkLauncher @Inject constructor(
    linkAnalyticsComponentFactory: LinkAnalyticsComponent.Factory,
    @PaymentElementCallbackIdentifier paymentElementCallbackIdentifier: String,
    linkActivityContract: LinkActivityContract,
    linkStore: LinkStore,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val linkAccountHolder: LinkAccountHolder,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val stateHolder: SheetActivityStateHolder,
    @Named(DEFAULT_CONTINUE_COORDINATOR) private val continueCoordinator: SheetActivityContinueCoordinator,
) : SheetLinkPaymentMethodLauncher {
    private val linkPaymentLauncher = LinkPaymentLauncher(
        linkAnalyticsComponentFactory,
        paymentElementCallbackIdentifier,
        linkActivityContract,
        linkStore,
    )

    override fun register(
        activityResultCaller: ActivityResultCaller,
        lifecycleOwner: LifecycleOwner,
    ) {
        linkPaymentLauncher.register(activityResultCaller, ::onLinkResult)
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkPaymentLauncher.unregister()
                }
            }
        )
    }

    override fun present(linkConfiguration: LinkConfiguration) {
        linkPaymentLauncher.present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = null),
            linkAccountInfo = linkAccountHolder.linkAccountInfo.value,
            linkExpressMode = LinkExpressMode.ENABLED,
            // Selection-only launch; Link returns a selection here and never confirms, so there is no auth surface
            // to color.
            statusBarColor = null,
        )
    }

    fun onLinkResult(result: LinkActivityResult) {
        result.linkAccountUpdate?.updateLinkAccount(linkAccountHolder)
        when (result) {
            is LinkActivityResult.Canceled -> Unit
            is LinkActivityResult.Failed -> stateHolder.updateError(result.error.stripeErrorMessage())
            is LinkActivityResult.Completed -> {
                val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return
                selectionHolder.setSelection(
                    PaymentSelection.Link(
                        brand = linkConfiguration.effectiveLinkBrand(linkAccountHolder.linkAccountInfo.value.account),
                        selectedPayment = result.selectedPayment,
                        shippingAddress = result.shippingAddress,
                    )
                )
                continueCoordinator.onContinue()
            }
            is LinkActivityResult.PaymentMethodObtained -> {
                val error = IllegalStateException(
                    "PaymentMethodObtained is not expected from authentication only Link flows"
                )
                stateHolder.updateError(error.stripeErrorMessage())
            }
        }
    }
}
