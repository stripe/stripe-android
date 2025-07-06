package com.stripe.android.link

import com.stripe.android.model.ConsumerPaymentDetails

/**
 * State container for Link payment details.
 * When null, payment details have not been loaded yet.
 * When non-null, contains the loaded payment details (which may be an empty list).
 */
internal data class ConsumerState(
    val paymentDetails: List<LinkPaymentMethod>
) {

    /**
     * Merges the backend payment details response with locally cached data.
     *
     * For payment details that exist in cache (matched by ID):
     * - Updates the [ConsumerPaymentDetails.PaymentDetails] with fresh backend data
     * - Preserves local fields like [collectedCvc] and [billingPhone]
     *
     * For new payment details from the response:
     * - Creates new [LinkPaymentMethod] with null local fields
     */
    fun withPaymentDetailsResponse(response: ConsumerPaymentDetails): ConsumerState {
        val existingById = paymentDetails.associateBy { it.details.id }
        return copy(
            paymentDetails = response.paymentDetails.map { details ->
                existingById[details.id]
                    ?.copy(details = details)
                    ?: LinkPaymentMethod(details, null, null)
            }
        )
    }

    /**
     * Updates a single payment detail in the state while preserving all local data.
     *
     * If a billing phone is provided, it will also be applied to all other payment details
     * that don't currently have a billing phone.
     *
     * @param updatedPayment The updated payment detail from the backend
     * @param billingPhone The billing phone to set, or null to preserve existing
     * @return A new [ConsumerState] with the updated payment detail, or the same state if no match found
     */
    fun withUpdatedPaymentDetail(
        updatedPayment: ConsumerPaymentDetails.PaymentDetails,
        billingPhone: String?
    ): ConsumerState {
        return copy(
            paymentDetails = paymentDetails.map { paymentDetail ->
                if (paymentDetail.details.id == updatedPayment.id) {
                    // Update the matching payment detail
                    paymentDetail.copy(
                        details = updatedPayment,
                        billingPhone = billingPhone ?: paymentDetail.billingPhone
                    )
                } else {
                    // Apply phone to payment details that don't have one, preserving existing if any
                    paymentDetail.copy(billingPhone = paymentDetail.billingPhone ?: billingPhone)
                }
            }
        )
    }

    companion object {
        /**
         * Creates a [ConsumerState] from a backend response with no cached data.
         */
        fun fromResponse(response: ConsumerPaymentDetails): ConsumerState {
            return ConsumerState(
                paymentDetails = response.paymentDetails.map { detail ->
                    LinkPaymentMethod(detail, null, null)
                }
            )
        }
    }
}
