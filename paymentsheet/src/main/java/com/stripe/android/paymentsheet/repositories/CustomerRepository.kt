package com.stripe.android.paymentsheet.repositories

import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet

/**
 * Interface for fetching and modifying information about a Customer.
 */
internal interface CustomerRepository {
    /**
     * Retrieve a Customer by ID using an ephemeral key.
     */
    suspend fun retrieveCustomer(
        customerId: String,
        ephemeralKeySecret: String
    ): Customer?

    /**
     * Retrieve a Customer's payment methods of all types requested.
     * @param silentlyFail Silently handle failures by returning an empty list for the payment method
     * types that failed.
     */
    suspend fun getPaymentMethods(
        customerConfig: PaymentSheet.CustomerConfiguration,
        types: List<PaymentMethod.Type>,
        silentlyFail: Boolean,
    ): Result<List<PaymentMethod>>

    /**
     * Detach a payment method from the Customer and return the modified [PaymentMethod].
     */
    suspend fun detachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): Result<PaymentMethod>

    /**
     * Attach a payment method to the Customer and return the modified [PaymentMethod].
     */
    suspend fun attachPaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String
    ): Result<PaymentMethod>

    suspend fun updatePaymentMethod(
        customerConfig: PaymentSheet.CustomerConfiguration,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): Result<PaymentMethod>
}
