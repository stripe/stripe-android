package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.PaymentMethod

/**
 * Removes the provided saved payment method alongside any duplicate stored payment methods. This function should be
 * deleted once an endpoint is stood up to handle detaching and removing duplicates.
 *
 * @param customerInfo authentication information that can perform detaching operations.
 * @param paymentMethodId the id of the payment method to remove and to compare with for stored duplicates
 *
 * @return a list of removal results that identify the payment methods that were successfully removed and the payment
 * methods that failed to be removed.
 */
internal suspend fun CustomerRepository.detachPaymentMethodAndDuplicates(
    customerInfo: CustomerRepository.CustomerInfo,
    paymentMethodId: String
): List<PaymentMethodRemovalResult> {
    val paymentMethods = getPaymentMethods(
        customerInfo = customerInfo,
        // We only support removing duplicate cards.
        types = listOf(PaymentMethod.Type.Card),
        silentlyFail = false,
    ).getOrElse {
        return listOf(
            PaymentMethodRemovalResult(
                paymentMethodId = paymentMethodId,
                result = Result.failure(it)
            )
        )
    }

    val requestedPaymentMethodToRemove = paymentMethods.find { paymentMethod ->
        paymentMethod.id == paymentMethodId
    } ?: return listOf()

    val paymentMethodRemovalResults = mutableListOf<PaymentMethodRemovalResult>()

    val paymentMethodsToRemove = paymentMethods.filter { paymentMethod ->
        paymentMethod.type == PaymentMethod.Type.Card &&
            paymentMethod.card?.fingerprint == requestedPaymentMethodToRemove.card?.fingerprint
    }

    paymentMethodsToRemove.forEach { paymentMethod ->
        val paymentMethodIdToRemove = paymentMethod.id!!

        detachPaymentMethod(
            customerInfo = customerInfo,
            paymentMethodId = paymentMethodIdToRemove
        ).onSuccess { removedPaymentMethod ->
            paymentMethodRemovalResults.add(
                PaymentMethodRemovalResult(
                    paymentMethodId = paymentMethodIdToRemove,
                    result = Result.success(removedPaymentMethod),
                )
            )
        }.onFailure {
            paymentMethodRemovalResults.add(
                PaymentMethodRemovalResult(
                    paymentMethodId = paymentMethodIdToRemove,
                    result = Result.failure(it),
                )
            )
        }
    }

    return paymentMethodRemovalResults
}

internal data class PaymentMethodRemovalResult(
    val paymentMethodId: String,
    val result: Result<PaymentMethod>,
)
