package com.stripe.android.link

import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.model.AccountStatus
import com.stripe.android.link.model.toLoginState
import com.stripe.android.link.utils.determineFallbackPaymentSelectionAfterLinkLogout
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.isLink
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkDisabledState
import com.stripe.android.paymentsheet.state.LinkState

internal fun handleLinkPaymentMethodSelectionResult(
    result: LinkActivityResult,
    selection: PaymentSelection?,
    customerState: CustomerState?,
    paymentMethodMetadata: PaymentMethodMetadata,
    currentLinkAccountInfo: LinkAccountUpdate.Value,
): List<LinkPaymentMethodSelectionOutcome> = buildList {
    val accountUpdate = result.linkAccountUpdate as? LinkAccountUpdate.Value
    if (accountUpdate != null) {
        add(
            LinkPaymentMethodSelectionOutcome.UpdatedLinkMetadata(
                linkAccountInfo = accountUpdate,
                paymentMethodMetadata = paymentMethodMetadata.withLinkAccount(accountUpdate),
            )
        )
    }
    val effectiveAccountInfo = accountUpdate ?: currentLinkAccountInfo

    when (result) {
        is LinkActivityResult.PaymentMethodObtained,
        is LinkActivityResult.Failed -> add(LinkPaymentMethodSelectionOutcome.Dismiss)
        is LinkActivityResult.Completed -> add(completionOutcome(selection, result))
        is LinkActivityResult.Canceled -> when (result.reason) {
            Reason.BackPressed -> {
                if (effectiveAccountInfo.account?.accountStatus == AccountStatus.VerificationStarted) {
                    add(LinkPaymentMethodSelectionOutcome.SuppressFutureEagerPresentation)
                }
                add(
                    if (selection.readyToPayWithLink()) {
                        LinkPaymentMethodSelectionOutcome.Dismiss
                    } else {
                        LinkPaymentMethodSelectionOutcome.ShowPaymentOptions
                    }
                )
            }
            Reason.LoggedOut -> add(logoutOutcome(selection, customerState, paymentMethodMetadata))
            Reason.PayAnotherWay -> add(LinkPaymentMethodSelectionOutcome.ShowPaymentOptions)
        }
    }
}

private fun completionOutcome(
    selection: PaymentSelection?,
    result: LinkActivityResult.Completed,
): LinkPaymentMethodSelectionOutcome {
    return if (selection is PaymentSelection.Link) {
        LinkPaymentMethodSelectionOutcome.UpdateSelection(
            selection = selection.copy(selectedPayment = result.selectedPayment),
            isCanceled = false,
            showPaymentOptions = false,
        )
    } else {
        LinkPaymentMethodSelectionOutcome.Dismiss
    }
}

private fun logoutOutcome(
    selection: PaymentSelection?,
    customerState: CustomerState?,
    paymentMethodMetadata: PaymentMethodMetadata,
): LinkPaymentMethodSelectionOutcome {
    return if (selection is PaymentSelection.Link) {
        LinkPaymentMethodSelectionOutcome.UpdateSelection(
            selection = determineFallbackPaymentSelectionAfterLinkLogout(
                customerState = customerState,
                paymentMethodMetadata = paymentMethodMetadata,
            ),
            isCanceled = true,
            showPaymentOptions = true,
        )
    } else {
        LinkPaymentMethodSelectionOutcome.ShowPaymentOptions
    }
}

private fun PaymentMethodMetadata.withLinkAccount(
    accountUpdate: LinkAccountUpdate.Value,
): PaymentMethodMetadata {
    val accountStatus = accountUpdate.account?.accountStatus ?: AccountStatus.SignedOut
    val updatedLinkState = when (val currentLinkState = linkStateResult) {
        is LinkState -> currentLinkState.copy(loginState = accountStatus.toLoginState())
        is LinkDisabledState, null -> currentLinkState
    }
    return copy(linkStateResult = updatedLinkState)
}

private fun PaymentSelection?.readyToPayWithLink(): Boolean {
    return when (this) {
        is PaymentSelection.Link -> selectedPayment != null
        null -> false
        else -> isLink
    }
}

internal sealed interface LinkPaymentMethodSelectionOutcome {
    data object Dismiss : LinkPaymentMethodSelectionOutcome
    data object ShowPaymentOptions : LinkPaymentMethodSelectionOutcome
    data object SuppressFutureEagerPresentation : LinkPaymentMethodSelectionOutcome

    data class UpdateSelection(
        val selection: PaymentSelection?,
        val isCanceled: Boolean,
        val showPaymentOptions: Boolean,
    ) : LinkPaymentMethodSelectionOutcome

    data class UpdatedLinkMetadata(
        val linkAccountInfo: LinkAccountUpdate.Value,
        val paymentMethodMetadata: PaymentMethodMetadata,
    ) : LinkPaymentMethodSelectionOutcome
}
