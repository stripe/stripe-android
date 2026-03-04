package com.stripe.android.paymentsheet.repositories

import android.os.Parcelable
import com.stripe.android.model.Customer
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import kotlinx.parcelize.Parcelize

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
     * Retrieve a Customer's payment methods using explicit params (no CustomerInfo).
     */
    suspend fun getPaymentMethods(
        customerId: String,
        ephemeralKeySecret: String,
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
        customerInfo: CustomerInfo,
        paymentMethodId: String
    ): Result<PaymentMethod>

    suspend fun updatePaymentMethod(
        customerInfo: CustomerInfo,
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): Result<PaymentMethod>

    suspend fun setDefaultPaymentMethod(
        customerInfo: CustomerInfo,
        paymentMethodId: String?,
    ): Result<Customer>

    @Parcelize
    data class CustomerInfo(
        val id: String,
        val ephemeralKeySecret: String,
        val customerSessionClientSecret: String?,
    ) : Parcelable
}
