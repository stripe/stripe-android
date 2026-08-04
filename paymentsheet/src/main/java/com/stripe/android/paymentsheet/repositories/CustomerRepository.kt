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
        stripeAccountId: String?,
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
        stripeAccountId: String?,
    ): Result<List<PaymentMethod>>

    /**
     * Detach a payment method from the Customer using a legacy ephemeral key.
     * Only detaches the specified payment method — no duplicate removal is needed because
     * legacy ephemeral keys don't filter out duplicates at display time.
     */
    suspend fun detachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        stripeAccountId: String?,
    ): Result<PaymentMethod>

    /**
     * Detach a payment method from the Customer using a customer session.
     * Also removes any duplicate payment methods with the same card fingerprint.
     */
    suspend fun detachPaymentMethodAndDuplicates(
        customerId: String,
        ephemeralKeySecret: String,
        customerSessionClientSecret: String,
        paymentMethodId: String,
        stripeAccountId: String?,
    ): Result<PaymentMethod>

    /**
     * Attach a payment method to the Customer and return the modified [PaymentMethod].
     */
    suspend fun attachPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        stripeAccountId: String?,
    ): Result<PaymentMethod>

    suspend fun updatePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
        stripeAccountId: String?,
    ): Result<PaymentMethod>

    suspend fun setDefaultPaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String?,
        stripeAccountId: String?,
    ): Result<Customer>

    suspend fun retrievePaymentMethod(
        customerId: String,
        ephemeralKeySecret: String,
        paymentMethodId: String,
        stripeAccountId: String?,
    ): Result<PaymentMethod>
}
