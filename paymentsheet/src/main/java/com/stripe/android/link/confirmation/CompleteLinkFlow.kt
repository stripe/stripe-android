package com.stripe.android.link.confirmation

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkAccountUpdate.Value
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.account.loadDefaultShippingAddress
import com.stripe.android.link.confirmation.CompleteLinkFlow.Result
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.withDismissalDisabled
import javax.inject.Inject
import com.stripe.android.link.confirmation.Result as LinkConfirmationResult

/**
 * Completes the Link flow with a given payment based on the [LinkLaunchMode].
 *
 * - On [LinkLaunchMode.Full] and [LinkLaunchMode.Confirmation], confirms the payment.
 * - On [LinkLaunchMode.PaymentMethodSelection], it returns the selected payment details.
 */
internal interface CompleteLinkFlow {
    /**
     * Confirms payment with the given payment details and CVC.
     */
    suspend operator fun invoke(
        selectedPaymentDetails: LinkPaymentMethod,
        linkAccount: LinkAccount
    ): Result

    sealed interface Result {
        /**
         * Completed successfully. Contains the appropriate
         * LinkActivityResult to be returned when closing the Link flow.
         */
        data class Completed(val linkActivityResult: LinkActivityResult) : Result

        /**
         * Completion was cancelled.
         */
        data object Canceled : Result

        /**
         * Completion failed with an error.
         */
        data class Failed(val error: ResolvableString) : Result
    }
}

internal class DefaultCompleteLinkFlow @Inject constructor(
    private val linkConfirmationHandler: LinkConfirmationHandler,
    private val linkAccountManager: LinkAccountManager,
    private val dismissalCoordinator: LinkDismissalCoordinator,
    private val linkLaunchMode: LinkLaunchMode
) : CompleteLinkFlow {

    override suspend operator fun invoke(
        selectedPaymentDetails: LinkPaymentMethod,
        linkAccount: LinkAccount
    ): Result {
        return completeLinkFlow(
            linkLaunchMode = linkLaunchMode,
            confirmPayment = {
                when (selectedPaymentDetails) {
                    is LinkPaymentMethod.ConsumerPaymentDetails -> linkConfirmationHandler.confirm(
                        paymentDetails = selectedPaymentDetails.details,
                        linkAccount = linkAccount,
                        cvc = selectedPaymentDetails.collectedCvc,
                        billingPhone = selectedPaymentDetails.billingPhone
                    )
                    is LinkPaymentMethod.LinkPaymentDetails -> linkConfirmationHandler.confirm(
                        paymentDetails = selectedPaymentDetails.linkPaymentDetails,
                        linkAccount = linkAccount,
                        cvc = selectedPaymentDetails.collectedCvc,
                        billingPhone = selectedPaymentDetails.billingPhone
                    )
                }
            },
            paymentMethodSelection = selectedPaymentDetails
        )
    }

    private suspend fun completeLinkFlow(
        linkLaunchMode: LinkLaunchMode,
        confirmPayment: suspend () -> LinkConfirmationResult,
        paymentMethodSelection: LinkPaymentMethod,
    ): Result {
        return when (linkLaunchMode) {
            is LinkLaunchMode.Full,
            is LinkLaunchMode.Confirmation -> {
                val result = dismissalCoordinator.withDismissalDisabled { confirmPayment() }
                when (result) {
                    LinkConfirmationResult.Canceled -> Result.Canceled
                    is LinkConfirmationResult.Failed -> Result.Failed(result.message)
                    LinkConfirmationResult.Succeeded -> Result.Completed(
                        linkActivityResult = LinkActivityResult.Completed(
                            linkAccountUpdate = Value(null, PaymentConfirmed),
                            selectedPayment = null,
                        )
                    )
                }
            }
            is LinkLaunchMode.PaymentMethodSelection -> Result.Completed(
                linkActivityResult = LinkActivityResult.Completed(
                    linkAccountUpdate = linkAccountManager.linkAccountUpdate,
                    selectedPayment = paymentMethodSelection,
                    shippingAddress = linkAccountManager.loadDefaultShippingAddress(),
                )
            )
            LinkLaunchMode.Authentication -> TODO()
        }
    }
}
