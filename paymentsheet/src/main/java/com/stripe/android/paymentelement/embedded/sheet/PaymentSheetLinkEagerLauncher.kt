package com.stripe.android.paymentelement.embedded.sheet

import com.stripe.android.core.injection.ViewModelScope
import com.stripe.android.link.LinkExpressMode
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentelement.confirmation.ConfirmationHandler
import com.stripe.android.paymentelement.confirmation.link.LinkConfirmationOption
import com.stripe.android.payments.core.injection.STATUS_BAR_COLOR
import com.stripe.android.paymentsheet.CustomerStateHolder
import com.stripe.android.paymentsheet.LinkHandler
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
internal class PaymentSheetLinkEagerLauncher @Inject constructor(
    private val sheetActivityArgs: SheetActivityArgs,
    private val paymentMethodMetadata: PaymentMethodMetadata,
    private val customerStateHolder: CustomerStateHolder,
    private val linkHandler: LinkHandler,
    private val sheetActivityStateHolder: DefaultSheetActivityStateHolder,
    private val confirmationHandler: ConfirmationHandler,
    @param:ViewModelScope private val coroutineScope: CoroutineScope,
    @Named(STATUS_BAR_COLOR) private val statusBarColor: Int?,
) {
    private var hasInitialized = false

    fun launchIfNeeded() {
        if (sheetActivityArgs !is SheetActivityArgs.PaymentSheet) return
        if (hasInitialized) return
        hasInitialized = true

        coroutineScope.launch {
            if (confirmationHandler.state.value !is ConfirmationHandler.State.Idle) {
                linkHandler.setupLink(paymentMethodMetadata.linkState)
                return@launch
            }

            val shouldLaunchEagerly = linkHandler.setupLinkWithEagerLaunch(
                state = paymentMethodMetadata.linkState,
                customerPaymentMethods = customerStateHolder.paymentMethods.value,
            )
            if (!shouldLaunchEagerly) return@launch

            // Attestation suspends while the sheet remains interactive. A user confirmation wins if it started
            // during that gap; do not replace its arguments or in-progress selection with eager Link.
            if (confirmationHandler.state.value !is ConfirmationHandler.State.Idle) return@launch

            val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return@launch
            val linkSelection = PaymentSelection.Link(
                brand = paymentMethodMetadata.effectiveLinkBrand(
                    linkHandler.linkConfigurationCoordinator.accountFlow.value
                ),
                linkExpressMode = LinkExpressMode.ENABLED_NO_WEB_FALLBACK,
            )
            sheetActivityStateHolder.setInProgressSelection(linkSelection)
            confirmationHandler.start(
                ConfirmationHandler.Args(
                    confirmationOption = LinkConfirmationOption(
                        configuration = linkConfiguration,
                        linkLaunchMode = LinkLaunchMode.Full,
                        linkExpressMode = linkSelection.linkExpressMode,
                    ),
                    paymentMethodMetadata = paymentMethodMetadata,
                    statusBarColor = statusBarColor,
                )
            )
        }
    }
}
