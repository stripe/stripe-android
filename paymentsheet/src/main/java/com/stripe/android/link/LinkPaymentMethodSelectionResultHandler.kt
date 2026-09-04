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
import javax.inject.Inject

internal class LinkPaymentMethodSelectionResultHandler @Inject constructor() {
    fun handle(
        result: LinkActivityResult,
        selection: PaymentSelection?,
        customerState: CustomerState?,
        paymentMethodMetadata: PaymentMethodMetadata,
        currentLinkAccountInfo: LinkAccountUpdate.Value,
    ): List<Outcome> = buildList {
        val accountUpdate = result.linkAccountUpdate as? LinkAccountUpdate.Value
        if (accountUpdate != null) {
            add(
                Outcome.UpdatedLinkMetadata(
                    linkAccountInfo = accountUpdate,
                    paymentMethodMetadata = paymentMethodMetadata.withLinkAccount(accountUpdate),
                )
            )
        }
        val effectiveAccountInfo = accountUpdate ?: currentLinkAccountInfo

        when (result) {
            is LinkActivityResult.PaymentMethodObtained,
            is LinkActivityResult.Failed -> add(Outcome.Dismiss)
            is LinkActivityResult.Completed -> add(completionOutcome(selection, result))
            is LinkActivityResult.Canceled -> when (result.reason) {
                Reason.BackPressed -> {
                    if (effectiveAccountInfo.account?.accountStatus == AccountStatus.VerificationStarted) {
                        add(Outcome.SuppressFutureEagerPresentation)
                    }
                    add(if (selection.readyToPayWithLink()) Outcome.Dismiss else Outcome.ShowPaymentOptions)
                }
                Reason.LoggedOut -> add(logoutOutcome(selection, customerState, paymentMethodMetadata))
                Reason.PayAnotherWay -> add(Outcome.ShowPaymentOptions)
            }
        }
    }

    private fun completionOutcome(
        selection: PaymentSelection?,
        result: LinkActivityResult.Completed,
    ): Outcome {
        return if (selection is PaymentSelection.Link) {
            Outcome.UpdateSelection(
                selection = selection.copy(selectedPayment = result.selectedPayment),
                isCanceled = false,
                showPaymentOptions = false,
            )
        } else {
            Outcome.Dismiss
        }
    }

    private fun logoutOutcome(
        selection: PaymentSelection?,
        customerState: CustomerState?,
        paymentMethodMetadata: PaymentMethodMetadata,
    ): Outcome {
        return if (selection is PaymentSelection.Link) {
            Outcome.UpdateSelection(
                selection = determineFallbackPaymentSelectionAfterLinkLogout(
                    customerState = customerState,
                    paymentMethodMetadata = paymentMethodMetadata,
                ),
                isCanceled = true,
                showPaymentOptions = true,
            )
        } else {
            Outcome.ShowPaymentOptions
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

    sealed interface Outcome {
        data object Dismiss : Outcome
        data object ShowPaymentOptions : Outcome
        data object SuppressFutureEagerPresentation : Outcome

        data class UpdateSelection(
            val selection: PaymentSelection?,
            val isCanceled: Boolean,
            val showPaymentOptions: Boolean,
        ) : Outcome

        data class UpdatedLinkMetadata(
            val linkAccountInfo: LinkAccountUpdate.Value,
            val paymentMethodMetadata: PaymentMethodMetadata,
        ) : Outcome
    }
}
