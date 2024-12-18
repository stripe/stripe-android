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
    suspend fun retrieveCustomer(customerInfo: CustomerInfo): Customer?

    /**
     * Retrieve a Customer's payment methods of all types requested.
     * @param silentlyFail Silently handle failures by returning an empty list for the payment method
     * types that failed.
     */
    suspend fun getPaymentMethods(
        customerInfo: CustomerInfo,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>>

    /**
     * Detach a payment method from the Customer and return the modified [PaymentMethod].
     */
    suspend fun detachPaymentMethod(
        customerInfo: CustomerInfo,
        paymentMethodId: String,
        canRemoveDuplicates: Boolean
    ): Result<PaymentMethod>

    /**
     * Attach a payment method to the Customer and return the modified [PaymentMethod].
     */
    suspend fun attachPaymentMethod(
        customerInfo: CustomerInfo,
        paymentMethodId: String
    ): Result<PaymentMethod>

    suspend fun updatePaymentMethod(
        customerInfo: CustomerInfo,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): Result<PaymentMethod>

    data class CustomerInfo(
        val id: String,
        val ephemeralKeySecret: String,
        val customerSessionClientSecret: String?,
    )
}
