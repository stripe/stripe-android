package com.stripe.android.paymentsheet.customer

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.core.Logger
import com.stripe.android.core.injection.WeakMapInjectorRegistry
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor
import com.stripe.android.model.PaymentMethod
import com.stripe.android.networking.PaymentAnalyticsRequestFactory
import com.stripe.android.networking.StripeApiRepository
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.repositories.CustomerApiRepository
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.wallet.sheet.SavedPaymentMethodsController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

val Context.dataStore by preferencesDataStore(name = "customer_saved_payment_methods")

interface CustomerAdapter {

    val customerId: String
    val canCreateSetupIntents: Boolean
    val customerEphemeralKeyProvider: suspend () -> String
    val setupIntentClientSecretProvider: (suspend () -> String)?

    /**
     * Retrieves a list of payment methods attached to a customer
     */
    suspend fun fetchPaymentMethods(): List<PaymentMethod>

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
    suspend fun fetchSelectedPaymentMethodOption(): PersistablePaymentMethodOption

    /**
     * Returns a client secret configured to the attach a new payment method to a customer.
     * This will call your backend to retrieve a client secret if you have provided a setupIntentClientSecretProvider in the init call.
     */
    suspend fun setupIntentClientSecretForCustomerAttach(): String?
}

/**
 * A persistable payment method
 */
sealed class PersistablePaymentMethodOption(open val id: String) {
    object GooglePay : PersistablePaymentMethodOption("google_pay")
    object Link : PersistablePaymentMethodOption("link")
    data class StripeId(override val id: String) : PersistablePaymentMethodOption(id)

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

/**
 * Configuration for the [CustomerAdapter]
 * @param customerId, the customer's payment method to manage
 * @param canCreateSetupIntents, whether this backend adapter is able to create [SetupIntent]s
 * @param customerEphemeralKeyProvider, callback returning a customer ephemeral key
 * @param setupIntentClientSecretProvider, optional callback returning a [SetupIntent] client secret for the current customer
 */
data class CustomerAdapterConfig(
    val customerId: String,
    val canCreateSetupIntents: Boolean = true,
    val customerEphemeralKeyProvider: suspend () -> String,
    val setupIntentClientSecretProvider: (suspend () -> String)? = null
)

/**
 * The default implementation of [CustomerAdapter]
 */
class StripeCustomerAdapter internal constructor(
    private val context: Context,
    override val customerId: String,
    override val canCreateSetupIntents: Boolean,
    override val customerEphemeralKeyProvider: suspend () -> String,
    override val setupIntentClientSecretProvider: (suspend () -> String)?
) : CustomerAdapter {

    internal lateinit var customerRepository: CustomerRepository

    override suspend fun fetchPaymentMethods(): List<PaymentMethod> {
        val ephemeralKey = customerEphemeralKeyProvider()
        return customerRepository.getPaymentMethods(
            customerConfig = PaymentSheet.CustomerConfiguration(
                id = customerId,
                ephemeralKeySecret = ephemeralKey
            ),
            types = listOf(
                PaymentMethod.Type.Card,
                PaymentMethod.Type.Link,
            )
        )
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String) {
        val ephemeralKey = customerEphemeralKeyProvider()
        customerRepository.attachPaymentMethod(
            customerConfig = PaymentSheet.CustomerConfiguration(
                id = customerId,
                ephemeralKeySecret = ephemeralKey
            ),
            paymentMethodId = paymentMethodId
        )
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String) {
        val ephemeralKey = customerEphemeralKeyProvider()
        customerRepository.detachPaymentMethod(
            customerConfig = PaymentSheet.CustomerConfiguration(
                id = customerId,
                ephemeralKeySecret = ephemeralKey
            ),
            paymentMethodId = paymentMethodId
        )
    }

    override suspend fun setSelectedPaymentMethodOption(paymentOption: PersistablePaymentMethodOption) {
        context.dataStore.edit { customers ->
            customers[stringPreferencesKey(customerId)] = paymentOption.id
        }
    }

    override suspend fun fetchSelectedPaymentMethodOption(): PersistablePaymentMethodOption {
        return suspendCoroutine { continuation ->
            context.dataStore.data.map { customers ->
                customers[stringPreferencesKey(customerId)]?.let { id ->
                    val paymentMethod = PersistablePaymentMethodOption.fromId(id)
                    continuation.resume(paymentMethod)
                }
            }
        }
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): String? {
        return setupIntentClientSecretProvider?.let { provider ->
            if (canCreateSetupIntents) {
                provider()
            } else {
                null
            }
        }
    }

    companion object {

        @JvmSynthetic
        internal var instance: CustomerAdapter? = null
        fun initCustomerAdapter(
            context: Context,
            customerId: String,
            canCreateSetupIntents: Boolean,
            customerEphemeralKeyProvider: suspend () -> String,
            setupIntentClientSecretProvider: (suspend () -> String)?
        ) {
            instance = StripeCustomerAdapter(
                context = context,
                customerId = customerId,
                canCreateSetupIntents = canCreateSetupIntents,
                customerEphemeralKeyProvider = customerEphemeralKeyProvider,
                setupIntentClientSecretProvider = setupIntentClientSecretProvider
            ).apply {
                // DI stuff here?
                customerRepository
            }
        }

        @JvmStatic
        fun getInstance(): CustomerAdapter {
            return checkNotNull(instance) {
                "Attempted to get instance of CustomerAdapter without initialization."
            }
        }
    }
}
