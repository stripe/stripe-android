package com.stripe.android.customersheet.data

import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams

@ExperimentalCustomerSheetApi
internal interface CustomerSheetDataRepository {
    /**
     * Defines if this repository can create [com.stripe.android.model.SetupIntent] objects. Use this
     * to determine if [retrieveSetupIntentClientSecret] can be used.
     */
    val canCreateSetupIntents: Boolean

    /**
     * Defines the list of payment method types that can be displayed  to the customer.
     */
    val paymentMethodTypes: List<String>

    /**
     * Retrieves a list of payment methods attached to a customer.
     *
     * @return a list of [PaymentMethod] objects
     */
    suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>>

    /**
     * Loads session for [com.stripe.android.customersheet.CustomerSheet]
     *
     * @return a [CustomerSheetSession] instance
     */
    suspend fun loadCustomerSheetSession(): Result<CustomerSheetSession>

    /**
     * Attaches a payment method to a customer.
     *
     * @param paymentMethodId identifier of the payment method to attach to a customer
     *
     * @return the modified [PaymentMethod]
     */
    suspend fun attachPaymentMethod(paymentMethodId: String): Result<PaymentMethod>

    /**
     * Updates a payment method.
     *
     * @param paymentMethodId identifier of the payment method to update
     * @param params The [PaymentMethodUpdateParams] with payment method values to update
     *
     * @return The updated [PaymentMethod]
     */
    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): Result<PaymentMethod>

    /**
     * Detaches the given payment method from a customer.
     *
     * @param paymentMethodId, identifier of the payment method to detach from a customer
     *
     * @return the detached [PaymentMethod]
     */
    suspend fun detachPaymentMethod(paymentMethodId: String): Result<PaymentMethod>

    /**
     * Retrieve the saved payment method option from a data store.
     *
     * @return the saved [CustomerPaymentOption]
     */
    suspend fun retrieveSelectedPaymentOption(): Result<CustomerPaymentOption>

    /**
     * Saves the payment option to a data store.
     *
     * @param paymentOption the [CustomerPaymentOption] to save to the data store
     *
     * @return success if the [CustomerPaymentOption] was persisted, failure otherwise
     */
    suspend fun setSelectedPaymentOption(paymentOption: CustomerPaymentOption): Result<Unit>

    /**
     * Returns a [com.stripe.android.model.SetupIntent] client secret.
     *
     * @return the client secret for the [com.stripe.android.model.SetupIntent].
     */
    suspend fun retrieveSetupIntentClientSecret(): Result<String>
}
