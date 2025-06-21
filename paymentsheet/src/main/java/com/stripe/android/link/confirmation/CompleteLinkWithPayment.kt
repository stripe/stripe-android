package com.stripe.android.link.confirmation

import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkActivityResult.Canceled.Reason
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.loadDefaultShippingAddress
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.model.ConsumerPaymentDetails
import javax.inject.Inject
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

/**
 * Use case for confirming payments with Link.
 * Handles the confirmation flow for different launch modes.
 */
internal class CompleteLinkWithPayment @Inject constructor(
    private val linkConfirmationHandler: LinkConfirmationHandler,
    private val linkAccountManager: LinkAccountManager,
    private val dismissalCoordinator: LinkDismissalCoordinator,
) {

    /**
     * Confirms payment with the given payment details and CVC.
     *
     * @param selectedPaymentDetails The payment method to use
     * @param linkAccount The Link account
     * @param cvc The CVC code (optional)
     * @param linkLaunchMode The launch mode determining the confirmation behavior
     * @return The activity result
     */
    suspend operator fun invoke(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        linkLaunchMode: LinkLaunchMode,
    ): LinkActivityResult {
        return when (linkLaunchMode) {
            is LinkLaunchMode.Full,
            is LinkLaunchMode.Confirmation -> toPaymentConfirmationActivityResult(
                confirmationResult = dismissalCoordinator.withDismissalDisabled {
                    linkConfirmationHandler.confirm(
                        paymentDetails = selectedPaymentDetails,
                        linkAccount = linkAccount,
                        cvc = cvc
                    )
                },
                linkAccount = linkAccount
            )
            is LinkLaunchMode.PaymentMethodSelection -> toPaymentSelectionActivityResult(
                linkAccount = linkAccount,
                selectedPayment = LinkPaymentMethod.ConsumerPaymentDetails(
                    details = selectedPaymentDetails,
                    collectedCvc = cvc
                )
            )
        }
    }

    /**
     * Confirms payment with LinkPaymentDetails (for newly created payment methods).
     *
     * @param linkPaymentDetails The Link payment details to use
     * @param linkAccount The Link account
     * @param cvc The CVC code (optional)
     * @param linkLaunchMode The launch mode determining the confirmation behavior
     * @return The activity result
     */
    suspend operator fun invoke(
        linkPaymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        linkLaunchMode: LinkLaunchMode,
    ): LinkActivityResult {
        return when (linkLaunchMode) {
            is LinkLaunchMode.Full,
            is LinkLaunchMode.Confirmation -> toPaymentConfirmationActivityResult(
                confirmationResult = dismissalCoordinator.withDismissalDisabled {
                    linkConfirmationHandler.confirm(
                        paymentDetails = linkPaymentDetails,
                        linkAccount = linkAccount,
                        cvc = cvc
                    )
                },
                linkAccount = linkAccount
            )
            is LinkLaunchMode.PaymentMethodSelection -> toPaymentSelectionActivityResult(
                linkAccount = linkAccount,
                selectedPayment = LinkPaymentMethod.LinkPaymentDetails(
                    linkPaymentDetails = linkPaymentDetails,
                    collectedCvc = cvc
                )
            )
        }
    }

    private fun toPaymentConfirmationActivityResult(
        confirmationResult: LinkConfirmationResult,
        linkAccount: LinkAccount
    ): LinkActivityResult = when (confirmationResult) {
        LinkConfirmationResult.Canceled -> LinkActivityResult.Canceled(
            reason = Reason.BackPressed,
            linkAccountUpdate = LinkAccountUpdate.Value(linkAccount)
        )
        is LinkConfirmationResult.Failed -> LinkActivityResult.Failed(
            error = Exception(confirmationResult.message.toString()),
            linkAccountUpdate = LinkAccountUpdate.Value(linkAccount)
        )
        LinkConfirmationResult.Succeeded -> LinkActivityResult.Completed(
            // After confirmation, clear the link account state so further launches
            // require authenticating again.
            linkAccountUpdate = LinkAccountUpdate.Value(null, PaymentConfirmed),
            selectedPayment = null,
        )
    }

    private suspend fun toPaymentSelectionActivityResult(
        linkAccount: LinkAccount,
        selectedPayment: LinkPaymentMethod
    ): LinkActivityResult {
        return LinkActivityResult.Completed(
            linkAccountUpdate = LinkAccountUpdate.Value(linkAccount),
            selectedPayment = selectedPayment,
            shippingAddress = linkAccountManager.loadDefaultShippingAddress(),
        )
    }
}
