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
internal suspend fun CustomerRepository.detachCardPaymentMethodAndDuplicates(
    customerInfo: CustomerRepository.CustomerInfo,
    paymentMethodId: String,
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
    } ?: throw IllegalArgumentException("Payment method with id '$paymentMethodId' does not exist!")

    val paymentMethodRemovalResults = mutableListOf<PaymentMethodRemovalResult>()

    val paymentMethodsToRemove = paymentMethods.filter { paymentMethod ->
        paymentMethod.type == PaymentMethod.Type.Card &&
            paymentMethod.card?.fingerprint == requestedPaymentMethodToRemove.card?.fingerprint
    }

    paymentMethodsToRemove.forEachIndexed { index, paymentMethod ->
        val paymentMethodIdToRemove = paymentMethod.id

        val result = paymentMethodIdToRemove?.let { id ->
            detachPaymentMethod(
                customerInfo = customerInfo,
                paymentMethodId = id
            )
        } ?: Result.failure(
            NoPaymentMethodIdOnRemovalException(
                index = index,
                paymentMethod = paymentMethod
            )
        )

        paymentMethodRemovalResults.add(
            PaymentMethodRemovalResult(
                paymentMethodId = paymentMethodIdToRemove,
                result = result,
            )
        )
    }

    return paymentMethodRemovalResults
}

internal class NoPaymentMethodIdOnRemovalException(
    val index: Int,
    val paymentMethod: PaymentMethod,
) : Exception() {
    override val message: String =
        "A payment method at index '$index' with type '${paymentMethod.type}' does not have an ID!"
}

internal data class PaymentMethodRemovalResult(
    val paymentMethodId: String?,
    val result: Result<PaymentMethod>,
)
