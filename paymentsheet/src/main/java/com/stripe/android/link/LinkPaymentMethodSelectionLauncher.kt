package com.stripe.android.link

import com.stripe.android.link.gate.LinkGate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import javax.inject.Inject

internal class LinkPaymentMethodSelectionLauncher @Inject constructor(
    private val linkGateFactory: LinkGate.Factory,
) {
    fun launchIfEligible(
        launcher: LinkPaymentLauncher,
        selection: PaymentSelection?,
        configuration: LinkConfiguration?,
        paymentMethodMetadata: PaymentMethodMetadata,
        linkAccountInfo: LinkAccountUpdate.Value,
        hasUserDeclinedVerification: Boolean,
        statusBarColor: Int?,
    ): Boolean {
        if (!isEligible(selection, configuration, linkAccountInfo, hasUserDeclinedVerification)) {
            return false
        }
        val linkSelection = requireNotNull(selection as? PaymentSelection.Link)
        val linkConfiguration = requireNotNull(configuration)

        launcher.present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountInfo,
            linkExpressMode = LinkExpressMode.ENABLED,
            launchMode = LinkLaunchMode.PaymentMethodSelection(
                selectedPayment = linkSelection.selectedPayment?.details,
            ),
            statusBarColor = statusBarColor,
        )
        return true
    }

    private fun isEligible(
        selection: PaymentSelection?,
        configuration: LinkConfiguration?,
        linkAccountInfo: LinkAccountUpdate.Value,
        hasUserDeclinedVerification: Boolean,
    ): Boolean {
        return when {
            selection !is PaymentSelection.Link -> false
            configuration == null -> false
            linkAccountInfo.account == null -> false
            hasUserDeclinedVerification -> false
            else -> linkGateFactory.create(configuration).showRuxInFlowController
        }
    }
}
