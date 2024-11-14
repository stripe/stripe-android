package com.stripe.android.customersheet

import android.content.Context
import com.stripe.android.customersheet.injection.DaggerStripeCustomerAdapterComponent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection

/**
 * A bridge to your backend to fetch customer-related information. Typically,
 * you will not need to implement this interface yourself. You should instead use
 * [CustomerAdapter.create], which manages retrieving and updating a Stripe customer for you.
 *
 * The methods in this interface should act on a Stripe `Customer` object.
 *
 * Implement this interface if you would prefer retrieving and updating your Stripe customer object
 * via your own backend instead of using the default implementation.
 *
 * Use [CustomerAdapter.create] to create an instance of a [CustomerAdapter].
 */
interface CustomerAdapter {

    /**
     * Whether this backend adapter is able to create setup intents. A [SetupIntent] is recommended
     * when attaching a new card to a Customer, and required for non-card payments methods. If you
     * are implementing your own [CustomerAdapter], return true if
     * [setupIntentClientSecretForCustomerAttach] is implemented, Otherwise, return false.
     */
    val canCreateSetupIntents: Boolean

    /**
     * A list of payment method types to display to the customer.
     * Valid values include: "card", "us_bank_account"
     * If null or empty, the SDK will dynamically determine the payment methods using your Stripe Dashboard settings.
     */
    val paymentMethodTypes: List<String>?

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
     * Updates a payment method with the provided [PaymentMethodUpdateParams].
     *
     * @param paymentMethodId The payment method to update
     * @param params The [PaymentMethodUpdateParams]
     * @return The updated [PaymentMethod]
     */
    suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): Result<PaymentMethod>

    /**
     * Saves the payment option to a data store.
     * @param paymentOption the [PaymentOption] to save to the data store. If null, the selected
     * payment method option will be cleared from the data store.
     * @return success if the [PaymentOption] was persisted, failure otherwise.
     */
    suspend fun setSelectedPaymentOption(paymentOption: PaymentOption?): Result<Unit>

    /**
     * Retrieve the saved payment method option from a data store. If null, the customer does not
     * have a default saved payment method.
     * @return the saved [PaymentOption].
     */
    suspend fun retrieveSelectedPaymentOption(): Result<PaymentOption?>

    /**
     * Returns a [SetupIntent] client secret to attach a new payment method to a customer. This will
     * call your backend to retrieve a client secret if you have provided a
     * [setupIntentClientSecretProvider] in the [CustomerAdapter.create] call.
     * @return the client secret for the [SetupIntent].
     */
    suspend fun setupIntentClientSecretForCustomerAttach(): Result<String>

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
         * @param paymentMethodTypes, A list of payment method types to display to the customer.
         *  Valid values include: "card", "us_bank_account"
         *  If null or empty, the SDK will dynamically determine the payment methods using your Stripe Dashboard
         *  settings.
         */
        @JvmStatic
        @JvmOverloads
        fun create(
            context: Context,
            customerEphemeralKeyProvider: CustomerEphemeralKeyProvider,
            setupIntentClientSecretProvider: SetupIntentClientSecretProvider?,
            paymentMethodTypes: List<String>? = null
        ): CustomerAdapter {
            val component = DaggerStripeCustomerAdapterComponent.builder()
                .context(context.applicationContext)
                .customerEphemeralKeyProvider(customerEphemeralKeyProvider)
                .setupIntentClientSecretProvider(setupIntentClientSecretProvider)
                .paymentMethodTypes(paymentMethodTypes)
                .build()
            return component.stripeCustomerAdapter
        }
    }

    /**
     * A representation of a saved payment method, used for persisting the user's default payment
     * method.
     */
    sealed class PaymentOption(
        open val id: String
    ) {

        internal object GooglePay : PaymentOption("google_pay")

        internal object Link : PaymentOption("link")

        internal data class StripeId(override val id: String) : PaymentOption(id)

        internal fun toPaymentSelection(
            paymentMethodProvider: (paymentMethodId: String) -> PaymentMethod?,
        ): PaymentSelection? {
            return when (this) {
                is GooglePay -> {
                    PaymentSelection.GooglePay
                }

                is Link -> {
                    PaymentSelection.Link
                }

                is StripeId -> {
                    paymentMethodProvider(id)?.let {
                        PaymentSelection.Saved(it)
                    }
                }
            }
        }

        internal fun toSavedSelection(): SavedSelection {
            return when (this) {
                is GooglePay -> SavedSelection.GooglePay
                is Link -> SavedSelection.Link
                is StripeId -> SavedSelection.PaymentMethod(id)
            }
        }

        companion object {

            @JvmStatic
            fun fromId(id: String): PaymentOption {
                return when (id) {
                    "google_pay" -> GooglePay
                    "link" -> Link
                    else -> StripeId(id)
                }
            }

            internal fun SavedSelection.toPaymentOption(): PaymentOption? {
                return when (this) {
                    is SavedSelection.GooglePay -> GooglePay
                    is SavedSelection.Link -> Link
                    is SavedSelection.None -> null
                    is SavedSelection.PaymentMethod -> StripeId(id)
                }
            }

            internal fun PaymentSelection.toPaymentOption(): PaymentOption? {
                return when (this) {
                    is PaymentSelection.GooglePay -> GooglePay
                    is PaymentSelection.Saved -> StripeId(paymentMethod.id!!)
                    else -> null
                }
            }
        }
    }

    sealed class Result<T> {

        class Success<T> internal constructor(
            val value: T
        ) : Result<T>()

        class Failure<T> internal constructor(
            val cause: Throwable,
            val displayMessage: String? = null
        ) : Result<T>()

        companion object {
            @JvmStatic
            fun <T> success(value: T): Result<T> {
                return Success(value)
            }

            @JvmStatic
            fun <T> failure(cause: Throwable, displayMessage: String?): Result<T> {
                return Failure(cause, displayMessage)
            }
        }
    }
}

/**
 * Callback to provide the customer's ID and an ephemeral key from your server.
 * Return [CustomerAdapter.Result.failure] with a [CustomerAdapter.Result.Failure.displayMessage]
 * if something went wrong during the retrieval of the ephemeral key.
 */
fun interface CustomerEphemeralKeyProvider {

    suspend fun provideCustomerEphemeralKey(): CustomerAdapter.Result<CustomerEphemeralKey>
}

/**
 * Callback to provide the [SetupIntent] client secret given a customer ID from your server.
 * Return [CustomerAdapter.Result.failure] with a with a
 * [CustomerAdapter.Result.Failure.displayMessage]  if something went wrong during the retrieval of
 * the [SetupIntent] client secret.
 */
fun interface SetupIntentClientSecretProvider {

    suspend fun provideSetupIntentClientSecret(customerId: String): CustomerAdapter.Result<String>
}

class CustomerEphemeralKey internal constructor(
    internal val customerId: String,
    internal val ephemeralKey: String,
) {
    companion object {

        @JvmStatic
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
