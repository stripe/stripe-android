package com.stripe.android.customersheet

import android.content.Context
import androidx.annotation.RestrictTo
import com.stripe.android.customersheet.injection.DaggerStripeCustomerAdapterComponent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.model.SavedSelection

/**
 * [CustomerAdapter] A bridge to your backend to fetch Customer-related information. Typically,
 * you will not need to implement this interface yourself. You should instead use
 * [CustomerAdaper.create], which manages retrieving and updating a Stripe customer for you.
 *
 * The methods in this interface should act on a Stripe [Customer] object.
 *
 * Implement this interface if you would prefer retrieving and updating your Stripe customer object
 * via your own backend instead of using the default implementation.
 *
 * Use [CustomerAdapter.create] to create an instance of a [CustomerAdapter].
 */
@ExperimentalCustomerSheetApi
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
     * @return the modified [PaymentMethod].
     */
    suspend fun detachPaymentMethod(paymentMethodId: String): Result<PaymentMethod>

    /**
     * Saves the payment option to a data store.
     * @param paymentOption, the [PaymentOption] to save to the data store. If null, the selected
     * payment method option will be cleared from the data store.
     * @return whether the payment option was saved
     */
    suspend fun setSelectedPaymentOption(paymentOption: PaymentOption?): Result<Boolean>

    /**
     * Retrieve the saved payment method option from a data store. If null, the customer does not
     * have a default saved payment method.
     * @return the saved [PaymentOption]
     */
    suspend fun retrieveSelectedPaymentOption(): Result<PaymentOption?>

    /**
     * Returns a [SetupIntent] client secret to attach a new payment method to a customer. This will
     * call your backend to retrieve a client secret if you have provided a
     * [setupIntentClientSecretProvider] in the [CustomerAdapter.create] call.
     * @return the client secret for the [SetupIntent]
     */
    suspend fun setupIntentClientSecretForCustomerAttach(): Result<String>

    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    companion object {

        /**
         * Creates a default implementation of [CustomerAdapter] which uses Android
         * [SharedPreferences] as the data store to manage the customer's default saved payment
         * methods locally.
         *
         * @param context, the Application context
         * @param customerEphemeralKeyProvider, a callback to retrieve the customer id and
         * ephemeral key. The customer ID is used in this adapter to manage the customer's saved
         * payment methods.
         * @param setupIntentClientSecretProvider, a callback to retrieve the [SetupIntent] client
         * secret. The client secret is used in this adapter to attach a payment method with a
         * [SetupIntent].
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

    /**
     * A representation of a saved payment method, used for persisting the user's default payment
     * method.
     */
    @ExperimentalCustomerSheetApi
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class PaymentOption(
        open val id: String
    ) {

        internal object GooglePay : PaymentOption("google_pay")

        internal object Link : PaymentOption("link")

        internal data class StripeId(override val id: String) : PaymentOption(id)

        @ExperimentalCustomerSheetApi
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        companion object {
            fun fromId(id: String): PaymentOption {
                return when (id) {
                    "google_pay" -> GooglePay
                    "link" -> Link
                    else -> StripeId(id)
                }
            }

            internal fun PaymentOption.toSavedSelection(): SavedSelection {
                return when (this) {
                    is GooglePay -> SavedSelection.GooglePay
                    is Link -> SavedSelection.Link
                    is StripeId -> SavedSelection.PaymentMethod(id)
                }
            }

            internal fun SavedSelection.toPaymentOption():
                PaymentOption? {
                return when (this) {
                    is SavedSelection.GooglePay -> GooglePay
                    is SavedSelection.Link -> Link
                    is SavedSelection.None -> null
                    is SavedSelection.PaymentMethod -> StripeId(id)
                }
            }
        }
    }
}

/**
 * Callback to provide the customer's ID and an ephemeral key from your server.
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface CustomerEphemeralKeyProvider {

    suspend fun provide(): Result<CustomerEphemeralKey>
}

/**
 * Callback to provide the [SetupIntent] client secret given a customer ID from your server.
 */
@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface SetupIntentClientSecretProvider {

    suspend fun provide(customerId: String): Result<String>
}

@ExperimentalCustomerSheetApi
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class CustomerEphemeralKey internal constructor(
    internal val customerId: String,
    internal val ephemeralKey: String,
) {
    @ExperimentalCustomerSheetApi
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
