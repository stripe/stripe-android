package com.stripe.android.link

import com.stripe.android.link.account.LinkAccountHolder
import com.stripe.android.link.account.updateLinkAccount
import com.stripe.android.link.gate.LinkGate
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.utils.determineFallbackPaymentSelectionAfterLinkLogout
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState
import javax.inject.Inject

internal class LinkPaymentMethodSelectionCoordinator @Inject constructor(
    private val linkAccountHolder: LinkAccountHolder,
    private val linkGateFactory: LinkGate.Factory,
) {
    fun launchIfEligible(
        launcher: LinkPaymentLauncher,
        paymentMethodMetadata: PaymentMethodMetadata,
        selection: PaymentSelection?,
        hasDeclinedLink2FA: Boolean,
        statusBarColor: Int?,
    ): Boolean {
        val linkConfiguration = paymentMethodMetadata.linkState?.configuration ?: return false
        val linkSelection = selection as? PaymentSelection.Link ?: return false
        val linkAccountInfo = linkAccountHolder.linkAccountInfo.value
        val shouldLaunch = !hasDeclinedLink2FA &&
            linkAccountInfo.account != null &&
            linkGateFactory.create(linkConfiguration).showRuxInFlowController

        if (!shouldLaunch) return false

        launcher.present(
            configuration = linkConfiguration,
            paymentMethodMetadata = paymentMethodMetadata,
            linkAccountInfo = linkAccountInfo,
            launchMode = LinkLaunchMode.PaymentMethodSelection(linkSelection.selectedPayment?.details),
            linkExpressMode = LinkExpressMode.ENABLED,
            statusBarColor = statusBarColor,
        )
        return true
    }

    fun handleResult(
        result: LinkActivityResult,
        paymentMethodMetadata: PaymentMethodMetadata,
        customer: CustomerState?,
        selection: PaymentSelection?,
    ): Outcome {
        val updatedPaymentMethodMetadata = result.linkAccountUpdate?.let { update ->
            update.updateLinkAccount(linkAccountHolder)
            update.updatedPaymentMethodMetadata(paymentMethodMetadata)
        }
        val action = when (result) {
            is LinkActivityResult.PaymentMethodObtained,
            is LinkActivityResult.Failed -> Action.Dismiss
            is LinkActivityResult.Canceled -> when (result.reason) {
                LinkActivityResult.Canceled.Reason.BackPressed -> {
                    if (selection?.readyToPayWithLink() == false) {
                        Action.ShowPaymentOptions
                    } else {
                        Action.Dismiss
                    }
                }
                LinkActivityResult.Canceled.Reason.LoggedOut -> selectionUpdate(
                    selection = selection,
                    linkPaymentMethod = null,
                    customer = customer,
                    paymentMethodMetadata = paymentMethodMetadata,
                    reason = SelectionUpdateReason.LoggedOut,
                )
                LinkActivityResult.Canceled.Reason.PayAnotherWay -> Action.ShowPaymentOptions
            }
            is LinkActivityResult.Completed -> selectionUpdate(
                selection = selection,
                linkPaymentMethod = result.selectedPayment,
                customer = customer,
                paymentMethodMetadata = paymentMethodMetadata,
                reason = SelectionUpdateReason.Completed,
            )
        }
        return Outcome(
            action = action,
            updatedPaymentMethodMetadata = updatedPaymentMethodMetadata,
            suppressFutureLinkLaunch = result is LinkActivityResult.Canceled &&
                result.reason == LinkActivityResult.Canceled.Reason.BackPressed &&
                linkAccountHolder.linkAccountInfo.value.account?.accountStatus == AccountStatus.VerificationStarted,
        )
    }

    private fun selectionUpdate(
        selection: PaymentSelection?,
        linkPaymentMethod: LinkPaymentMethod?,
        customer: CustomerState?,
        paymentMethodMetadata: PaymentMethodMetadata,
        reason: SelectionUpdateReason,
    ): Action {
        val linkSelection = selection as? PaymentSelection.Link ?: return Action.Dismiss
        val updatedSelection = if (linkPaymentMethod != null) {
            linkSelection.copy(selectedPayment = linkPaymentMethod)
        } else {
            determineFallbackPaymentSelectionAfterLinkLogout(
                customer = customer,
                paymentMethodMetadata = paymentMethodMetadata,
            )
        }
        return Action.UpdateSelection(updatedSelection, reason)
    }

    private fun PaymentSelection.readyToPayWithLink(): Boolean = when (this) {
        is PaymentSelection.Link -> selectedPayment != null
        else -> isLink
    }

    private fun LinkAccountUpdate.updatedPaymentMethodMetadata(
        paymentMethodMetadata: PaymentMethodMetadata,
    ): PaymentMethodMetadata? {
        if (this !is LinkAccountUpdate.Value) return null

        val accountStatus = account?.accountStatus ?: AccountStatus.SignedOut
        val updatedLinkState = when (val linkState = paymentMethodMetadata.linkStateResult) {
            is LinkState -> linkState.copy(loginState = accountStatus.toLoginState())
            is LinkDisabledState, null -> linkState
        }
        return paymentMethodMetadata.copy(linkStateResult = updatedLinkState)
    }

    data class Outcome(
        val action: Action,
        val updatedPaymentMethodMetadata: PaymentMethodMetadata?,
        val suppressFutureLinkLaunch: Boolean,
    )

    sealed interface Action {
        data object Dismiss : Action
        data object ShowPaymentOptions : Action
        data class UpdateSelection(
            val selection: PaymentSelection?,
            val reason: SelectionUpdateReason,
        ) : Action
    }

    enum class SelectionUpdateReason {
        Completed,
        LoggedOut,
    }
}
