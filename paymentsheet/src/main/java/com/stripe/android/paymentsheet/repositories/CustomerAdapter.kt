package com.stripe.android.paymentsheet.repositories

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.injection.DaggerStripeCustomerAdapterComponent

/**
 * [CustomerAdapter] A "bridge" from wallet mode to your backend to fetch Customer-related
 * information. Typically, you will not need to implement this interface yourself. You should
 * instead use [CustomerAdaper.create], which manages retrieving and updating a Stripe customer for
 * you.
 *
 * Implement this interface if you would prefer retrieving and updating your Stripe customer object
 * via your own backend instead of using the default implementation.
 *
 * Use [CustomerAdapter.create] to create an instance of a [CustomerAdapter].
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface CustomerAdapter {
    /**
     * Retrieves a list of payment methods attached to a customer
     * @return a list of [PaymentMethod]s.
     */
    suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>>

    /**
     * Attaches a payment method to a customer
     * @param paymentMethodId, the payment method to attach to a customer
     * @return the modified [PaymentMethod].
     */
    suspend fun attachPaymentMethod(paymentMethodId: String): Result<PaymentMethod>

    /**
     * Detaches the given payment method from a customer
     * @param paymentMethodId, the payment method to detach from the customer
     */
    suspend fun detachPaymentMethod(paymentMethodId: String)

    /**
     * Set the selected payment method option.
     * @param paymentOption, the [PersistablePaymentMethodOption] to save to the data store. If
     * null, the selected payment method option will be cleared from the data store.
     */
    suspend fun setSelectedPaymentMethodOption(paymentOption: PersistablePaymentMethodOption?)

    /**
     * Fetch the persisted payment method option from the data store. If null, the customer does
     * not have a default saved payment method.
     */
    suspend fun fetchSelectedPaymentMethodOption(): Result<PersistablePaymentMethodOption?>

    /**
     * Returns a client secret configured to attach a new payment method to a customer.
     * This will call your backend to retrieve a client secret if you have provided a
     * setupIntentClientSecretProvider in the init call.
     */
    suspend fun setupIntentClientSecretForCustomerAttach(): Result<String>

    @ExperimentalSavedPaymentMethodsApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        /**
         * Creates a default implementation of [CustomerAdapter] which uses Android
         * SharedPreferences as the data store to manage the customer's default saved payment
         * methods.
         *
         * @param context, the Application context
         * @param customerEphemeralKeyProvider, a callback to retrieve the customer id and
         * ephemeral key. The customer ID is used in this adapter to manage the customer's saved
         * payment methods.
         * @param setupIntentClientSecretProvider, a callback to retrieve the setup intent client
         * secret. The client secret is used in this adapter to attach a payment method with a
         * setup intent.
         */
        fun create(
            context: Context,
            customerEphemeralKeyProvider: CustomerEphemeralKeyProvider,
            setupIntentClientSecretProvider: SetupIntentClientSecretProvider?,
        ): CustomerAdapter {
            val component = DaggerStripeCustomerAdapterComponent.builder()
                .context(context)
                .customerEphemeralKeyProvider(customerEphemeralKeyProvider)
                .setupIntentClientSecretProvider(setupIntentClientSecretProvider)
                .build()
            return component.stripeCustomerAdapter
        }
    }
}

/**
 * A representation of a saved payment method, used for persisting the user's default payment
 * method.
 */
@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PersistablePaymentMethodOption(
    open val id: String
) {

    internal object GooglePay : PersistablePaymentMethodOption("google_pay")

    internal object Link : PersistablePaymentMethodOption("link")

    internal data class StripeId(override val id: String) : PersistablePaymentMethodOption(id)

    @ExperimentalSavedPaymentMethodsApi
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

@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CustomerEphemeralKeyProvider {

    suspend fun provide(): Result<CustomerEphemeralKey>
}

@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface SetupIntentClientSecretProvider {

    suspend fun provide(): Result<String>
}

@ExperimentalSavedPaymentMethodsApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CustomerEphemeralKey internal constructor(
    internal val customerId: String,
    internal val ephemeralKey: String,
) {
    @ExperimentalSavedPaymentMethodsApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {
        fun create(
            customerId: String,
            ephemeralKey: String,
        ): CustomerEphemeralKey {
            return CustomerEphemeralKey(
                customerId = customerId,
                ephemeralKey = ephemeralKey,
            )
        }
    }
}
