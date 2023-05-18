package com.stripe.android.paymentsheet.repositories

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.stripe.android.ExperimentalSavedPaymentMethodsApi
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.injection.CAN_CREATE_SETUP_INTENT
import javax.inject.Inject
import javax.inject.Named

@Suppress("unused")
private val Context.dataStore by preferencesDataStore(name = "customer_saved_payment_methods")

/**
 * The default implementation of [CustomerAdapter]. This adapter uses the customer ID and ephemeral
 * key provided by [CustomerEphemeralKeyProvider] to read, update, or delete the customer's
 * default saved payment method using Android [DataStore].
 */
@OptIn(ExperimentalSavedPaymentMethodsApi::class)
@Suppress("unused")
internal class StripeCustomerAdapter @Inject constructor(
    private val context: Context,
    private val customerEphemeralKeyProvider: CustomerEphemeralKeyProvider,
    private val setupIntentClientSecretProvider: SetupIntentClientSecretProvider?,
    @Named(CAN_CREATE_SETUP_INTENT) private val canCreateSetupIntents: Boolean,
    private val timeProvider: () -> Long,
    private val customerRepository: CustomerRepository,
) : CustomerAdapter {

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
}
