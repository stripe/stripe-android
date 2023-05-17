package com.stripe.android.paymentsheet.repositories

import androidx.annotation.RestrictTo
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.model.PaymentMethod

/**
 * [CustomerAdapter] A "bridge" from wallet mode to your backend to fetch Customer-related
 * information. Typically, you will not need to implement this interface yourself. You should
 * instead use [StripeCustomerAdapter], which implements [CustomerAdapter] and manages retrieving
 * and updating a Stripe customer for you.
 *
 * Implement this interface if you would prefer retrieving and updating your Stripe customer object
 * via your own backend instead of using [StripeCustomerAdapter].
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CustomerAdapter {
    /**
     * Retrieves a list of payment methods attached to a customer
     */
    suspend fun fetchPaymentMethods(): Result<List<PaymentMethod>>

    /**
     * Adds a payment method to a customer
     * @param paymentMethodId, the payment method to attach to the customer
     */
    suspend fun attachPaymentMethod(paymentMethodId: String)

    /**
     * Deletes the given payment method from the customer
     * @param paymentMethodId, the payment method to detach from the customer
     */
    suspend fun detachPaymentMethod(paymentMethodId: String)

    /**
     * Set the selected payment method option.
     * @param paymentOption, the [PersistablePaymentMethodOption] to save in the [DataStore]
     */
    suspend fun setSelectedPaymentMethodOption(paymentOption: PersistablePaymentMethodOption)

    /**
     * Fetch the persisted payment method option from the [DataStore]
     */
    suspend fun fetchSelectedPaymentMethodOption(): Result<PersistablePaymentMethodOption?>

    /**
     * Returns a client secret configured to the attach a new payment method to a customer.
     * This will call your backend to retrieve a client secret if you have provided a
     * setupIntentClientSecretProvider in the init call.
     */
    suspend fun setupIntentClientSecretForCustomerAttach(): Result<String>
}

/**
 * A representation of a saved payment method, used for persisting the user's default payment
 * method.
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PersistablePaymentMethodOption(open val id: String) {

    /**
     * GooglePay is the customer's default payment method.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object GooglePay : PersistablePaymentMethodOption("google_pay")

    /**
     * Link is the customer's default payment method.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    object Link : PersistablePaymentMethodOption("link")

    /**
     * A Stripe payment method backed by a Stripe PaymentMethod ID.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    data class StripeId(override val id: String) : PersistablePaymentMethodOption(id)

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun fromId(id: String): PersistablePaymentMethodOption {
            return when (id) {
                "google_pay" -> GooglePay
                "link" -> Link
                else -> StripeId(id)
            }
        }
    }
}
