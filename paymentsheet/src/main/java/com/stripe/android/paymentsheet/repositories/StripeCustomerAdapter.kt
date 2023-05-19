package com.stripe.android.paymentsheet.repositories

import android.content.Context
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.injection.CAN_CREATE_SETUP_INTENT
import javax.inject.Inject
import javax.inject.Named

/**
 * The default implementation of [CustomerAdapter]. This adapter uses the customer ID and ephemeral
 * key provided by [CustomerEphemeralKeyProvider] to read, update, or delete the customer's
 * default saved payment method using Android [SharedPreferences].
 */
@OptIn(ExperimentalSavedPaymentMethodsApi::class)
@Suppress("unused")
@JvmSuppressWildcards
internal class StripeCustomerAdapter @Inject constructor(
    private val context: Context,
    private val customerEphemeralKeyProvider: CustomerEphemeralKeyProvider,
    private val setupIntentClientSecretProvider: SetupIntentClientSecretProvider?,
    @Named(CAN_CREATE_SETUP_INTENT) private val canCreateSetupIntents: Boolean,
    private val timeProvider: () -> Long,
    private val customerRepository: CustomerRepository,
    private val prefsRepositoryFactory: (CustomerEphemeralKey) -> PrefsRepository,
) : CustomerAdapter {

    internal var cachedCustomer: Result<CustomerEphemeralKey>? = null
    private var cacheDate: Long? = null

    override suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>> {
        TODO()
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String) {
        TODO()
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String) {
        TODO()
    }

    override suspend fun setSelectedPaymentMethodOption(paymentOption: PersistablePaymentMethodOption?) {
        TODO()
    }

    override suspend fun fetchSelectedPaymentMethodOption(): Result<PersistablePaymentMethodOption> {
        TODO()
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): Result<String> {
        TODO()
    }

    internal suspend fun getCustomer(): Result<CustomerEphemeralKey> {
        return cachedCustomer.takeUnless {
            it == null || shouldRefreshCustomer()
        } ?: run {
            val updatedCustomer = customerEphemeralKeyProvider.provide()
            cacheDate = timeProvider()
            cachedCustomer = updatedCustomer
            updatedCustomer
        }
    }

    private fun shouldRefreshCustomer(): Boolean {
        val customerCreated = cacheDate ?: return true
        val nowInMillis = timeProvider()
        return customerCreated + CACHED_CUSTOMER_MAX_AGE_MILLIS < nowInMillis
    }

    internal companion object {
        // 30 minutes, server-side timeout is 60
        internal const val CACHED_CUSTOMER_MAX_AGE_MILLIS = 60 * 30 * 1000L
    }
}
