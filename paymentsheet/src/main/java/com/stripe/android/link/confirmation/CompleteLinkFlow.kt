package com.stripe.android.link.confirmation

import com.stripe.android.core.strings.ResolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.link.LinkAccountUpdate.Value.UpdateReason.PaymentConfirmed
import com.stripe.android.link.LinkActivityResult
import com.stripe.android.link.LinkDismissalCoordinator
import com.stripe.android.link.LinkLaunchMode
import com.stripe.android.link.LinkPaymentDetails
import com.stripe.android.link.LinkPaymentMethod
import com.stripe.android.link.account.LinkAccountManager
import com.stripe.android.link.account.linkAccountUpdate
import com.stripe.android.link.account.loadDefaultShippingAddress
import com.stripe.android.link.confirmation.CompleteLinkFlow.Result
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.link.withDismissalDisabled
import com.stripe.android.model.ConsumerPaymentDetails
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
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        linkLaunchMode: LinkLaunchMode,
    ): Result

    /**
     * Confirms payment with LinkPaymentDetails (for newly created payment methods).
     */
    suspend operator fun invoke(
        linkPaymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        linkLaunchMode: LinkLaunchMode,
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
) : CompleteLinkFlow {

    override suspend operator fun invoke(
        selectedPaymentDetails: ConsumerPaymentDetails.PaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        linkLaunchMode: LinkLaunchMode,
    ): Result {
        return completeLinkFlow(
            linkLaunchMode = linkLaunchMode,
            confirmPayment = {
                linkConfirmationHandler.confirm(
                    paymentDetails = selectedPaymentDetails,
                    linkAccount = linkAccount,
                    cvc = cvc
                )
            },
            createPaymentMethodSelection = {
                LinkPaymentMethod.ConsumerPaymentDetails(
                    details = selectedPaymentDetails,
                    collectedCvc = cvc
                )
            }
        )
    }

    override suspend operator fun invoke(
        linkPaymentDetails: LinkPaymentDetails,
        linkAccount: LinkAccount,
        cvc: String?,
        linkLaunchMode: LinkLaunchMode,
    ): Result {
        return completeLinkFlow(
            linkLaunchMode = linkLaunchMode,
            confirmPayment = {
                linkConfirmationHandler.confirm(
                    paymentDetails = linkPaymentDetails,
                    linkAccount = linkAccount,
                    cvc = cvc
                )
            },
            createPaymentMethodSelection = {
                LinkPaymentMethod.LinkPaymentDetails(
                    linkPaymentDetails = linkPaymentDetails,
                    collectedCvc = cvc,
                )
            }
        )
    }

    private suspend fun completeLinkFlow(
        linkLaunchMode: LinkLaunchMode,
        confirmPayment: suspend () -> LinkConfirmationResult,
        createPaymentMethodSelection: () -> LinkPaymentMethod,
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
                            linkAccountUpdate = LinkAccountUpdate.Value(null, PaymentConfirmed),
                            selectedPayment = null,
                        )
                    )
                }
            }
            is LinkLaunchMode.PaymentMethodSelection -> Result.Completed(
                linkActivityResult = LinkActivityResult.Completed(
                    linkAccountUpdate = linkAccountManager.linkAccountUpdate,
                    selectedPayment = createPaymentMethodSelection(),
                    shippingAddress = linkAccountManager.loadDefaultShippingAddress(),
                )
            )
        }
    }
}
