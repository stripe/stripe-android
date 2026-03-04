package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams

/**
 * Interface for fetching and modifying information about a Customer.
 */
internal interface CustomerRepository {
    /**
     * Retrieve a Customer by ID using an ephemeral key.
     */
    suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String,
    ): Customer?

    /**
     * Retrieve a Customer's payment methods of all types requested.
     * @param silentlyFail Silently handle failures by returning an empty list for the payment method
     * types that failed.
     */
    suspend fun getPaymentMethods(
        customerId: String,
        ephemeralKeySecret: String,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>>

    /**
     * Detach a payment method using legacy ephemeral key (no customer session).
     */
    suspend fun detachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod>

    /**
     * Detach a payment method using customer session client secret.
     */
    suspend fun detachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        customerSessionClientSecret: String,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean,
    ): Result<PaymentMethod>

    /**
     * Attach a payment method to the Customer and return the modified [PaymentMethod].
     */
    suspend fun attachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
    ): Result<PaymentMethod>

    /**
     * Update a payment method's details.
     */
    suspend fun updatePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod>

    /**
     * Set the default payment method for a customer.
     */
    suspend fun setDefaultPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String?,
    ): Result<Customer>
}
