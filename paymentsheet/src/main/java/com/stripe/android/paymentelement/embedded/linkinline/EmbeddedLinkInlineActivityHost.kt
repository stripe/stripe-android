package com.stripe.android.paymentelement.embedded.linkinline

import androidx.activity.result.ActivityResultRegistryOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentLauncher
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.LinkBrand
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationHelper
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedPaymentElementScope
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

/**
 * The parts of inline Link verification that need the screen hosting the Embedded Payment Element: opening the
 * Link wallet and paying with it.
 */
internal interface EmbeddedLinkInlineActivityHost {
    /**
     * Opens the Link wallet so the customer can pick what to pay with. The result goes to
     * [EmbeddedLinkInlineAuth.onWalletResult].
     */
    fun launchLinkWallet(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        selectedPayment: LinkPaymentMethod?,
    )

    /**
     * Pays in the Link wallet, the same way a form sheet pays with `FormSheetAction.Confirm`.
     */
    fun payWithLinkWallet(linkBrand: LinkBrand)
}

@EmbeddedPaymentElementScope
internal class DefaultEmbeddedLinkInlineActivityHost @Inject constructor(
    activityResultRegistryOwner: ActivityResultRegistryOwner,
    private val lifecycleOwner: LifecycleOwner,
    @Named(EMBEDDED_LINK_WALLET_LAUNCHER) private val linkPaymentLauncher: LinkPaymentLauncher,
    private val linkInlineAuth: EmbeddedLinkInlineAuth,
    private val selectionHolder: EmbeddedSelectionHolder,
    private val confirmationStateHolder: EmbeddedConfirmationStateHolder,
    private val confirmationHelper: EmbeddedConfirmationHelper,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
) : EmbeddedLinkInlineActivityHost {
    init {
        // A key of its own, so the wallet doesn't replace the Link launcher used to confirm.
        linkPaymentLauncher.register(
            key = EMBEDDED_LINK_WALLET_LAUNCHER,
            activityResultRegistry = activityResultRegistryOwner.activityResultRegistry,
            callback = linkInlineAuth::onWalletResult,
        )
        lifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    linkPaymentLauncher.unregister()
                }
            }
        )
    }

    override fun launchLinkWallet(
        configuration: LinkConfiguration,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        selectedPayment: LinkPaymentMethod?,
    ) {
        linkPaymentLauncher.present(
            configuration = configuration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountInfo,
            launchMode = LinkLaunchMode.PaymentMethodSelection(selectedPayment = selectedPayment?.details),
            linkExpressMode = LinkExpressMode.ENABLED,
            statusBarColor = statusBarColor,
        )
    }

    override fun payWithLinkWallet(linkBrand: LinkBrand) {
        lifecycleOwner.lifecycleScope.launch {
            // Waits for a configure in flight, which clears the state until it finishes.
            confirmationStateHolder.stateFlow.filterNotNull().first()
            val link = PaymentSelection.Link(brand = linkBrand)
            selectionHolder.setSelection(link)
            // The confirmation state picks up the selection asynchronously.
            confirmationStateHolder.stateFlow.first { it?.selection == link }
            confirmationHelper.confirm()
        }
    }
}

internal const val EMBEDDED_LINK_WALLET_LAUNCHER = "LinkPaymentLauncher_EmbeddedLinkWallet"
