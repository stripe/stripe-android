package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Interface for fetching and modifying information about a Customer.
 */
internal interface CustomerRepository {
    /**
     * Retrieve a Customer's payment methods of all types requested.
     * Silently handle failures by returning an empty list for the payment method types that failed.
     */
    suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>
    ): List<PaymentMethod>

    /**
     * Detach a payment method from the Customer and return the modified [PaymentMethod].
     * Silently handle failures by returning null.
     */
    suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): PaymentMethod?
}
