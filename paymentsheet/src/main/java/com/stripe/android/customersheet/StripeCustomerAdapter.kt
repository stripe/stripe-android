package com.stripe.android.customersheet

import android.content.Context
import com.stripe.android.common.coroutines.CoalescingOrchestrator
import com.stripe.android.common.exception.stripeErrorMessage
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.lpmfoundations.paymentmethod.CAN_REMOVE_DUPLICATES_CUSTOMER_SHEET_VALUE
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [CustomerAdapter]. This adapter uses the customer ID and ephemeral
 * key provided by [CustomerEphemeralKeyProvider] to read, update, or delete the customer's
 * default saved payment method using Android [SharedPreferences]. By default, this adapter saves
 * the customer's selected payment method to [SharedPreferences], which is used by [PaymentSheet]
 * to load the customer's default saved payment method.
 */
@JvmSuppressWildcards
internal class StripeCustomerAdapter @Inject internal constructor(
    private val context: Context,
    private val customerEphemeralKeyProvider: CustomerEphemeralKeyProvider,
    private val setupIntentClientSecretProvider: SetupIntentClientSecretProvider?,
    override val paymentMethodTypes: List<String>?,
    private val timeProvider: () -> Long,
    private val customerRepository: CustomerRepository,
    private val prefsRepositoryFactory: (CustomerEphemeralKey) -> PrefsRepository,
    @IOContext private val workContext: CoroutineContext,
) : CustomerAdapter {

    @Volatile
    private var cachedCustomerEphemeralKey: CachedCustomerEphemeralKey? = null

    private val customerEphemeralKeyCoalescingOrchestrator = CoalescingOrchestrator(
        factory = customerEphemeralKeyProvider::provideCustomerEphemeralKey,
    )

    override val canCreateSetupIntents: Boolean
        get() = setupIntentClientSecretProvider != null

    override suspend fun retrievePaymentMethods(): CustomerAdapter.Result<List<PaymentMethod>> {
        paymentMethodTypes?.filter { PaymentMethod.Type.fromCode(it) == null }?.takeIf { it.isNotEmpty() }?.run {
            val cause = IllegalStateException("Invalid payment method types provided (${joinToString()}).")
            return CustomerAdapter.Result.failure(cause, null)
        }

        val supportedPaymentMethodCodes = supportedPaymentMethodTypes.map { it.code }.toSet()
        val unsupportedPaymentMethods = paymentMethodTypes?.minus(supportedPaymentMethodCodes) ?: emptyList()
        if (unsupportedPaymentMethods.isNotEmpty()) {
            val unsupportedCodes = unsupportedPaymentMethods.joinToString()
            val cause = IllegalStateException("Unsupported payment method types provided ($unsupportedCodes).")
            return CustomerAdapter.Result.failure(cause, null)
        }

        val requestedTypes = if (paymentMethodTypes.isNullOrEmpty()) {
            supportedPaymentMethodTypes
        } else {
            paymentMethodTypes.mapNotNull { PaymentMethod.Type.fromCode(it) }
        }

        return getCustomerEphemeralKey().map { customerEphemeralKey ->
            customerRepository.getPaymentMethods(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey,
                    customerSessionClientSecret = null,
                ),
                types = requestedTypes,
                silentlyFail = false,
            ).getOrElse {
                return CustomerAdapter.Result.failure(
                    cause = it,
                    displayMessage = it.stripeErrorMessage(context),
                )
            }
        }
    }

    override suspend fun attachPaymentMethod(
        paymentMethodId: String
    ): CustomerAdapter.Result<PaymentMethod> {
        return getCustomerEphemeralKey().map { customerEphemeralKey ->
            customerRepository.attachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey,
                    customerSessionClientSecret = null,
                ),
                paymentMethodId = paymentMethodId
            ).getOrElse {
                return CustomerAdapter.Result.failure(
                    cause = it,
                    displayMessage = it.stripeErrorMessage(context),
                )
            }
        }
    }

    override suspend fun detachPaymentMethod(
        paymentMethodId: String
    ): CustomerAdapter.Result<PaymentMethod> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            customerRepository.detachPaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey,
                    customerSessionClientSecret = null,
                ),
                paymentMethodId = paymentMethodId,
                canRemoveDuplicates = CAN_REMOVE_DUPLICATES_CUSTOMER_SHEET_VALUE,
            ).getOrElse {
                return CustomerAdapter.Result.failure(
                    cause = it,
                    displayMessage = it.stripeErrorMessage(context),
                )
            }
        }
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams
    ): CustomerAdapter.Result<PaymentMethod> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            customerRepository.updatePaymentMethod(
                customerInfo = CustomerRepository.CustomerInfo(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey,
                    customerSessionClientSecret = null,
                ),
                paymentMethodId = paymentMethodId,
                params = params
            ).getOrElse {
                return CustomerAdapter.Result.failure(
                    cause = it,
                    displayMessage = it.stripeErrorMessage(context),
                )
            }
        }
    }

    override suspend fun setSelectedPaymentOption(
        paymentOption: CustomerAdapter.PaymentOption?
    ): CustomerAdapter.Result<Unit> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            val prefsRepository = prefsRepositoryFactory(customerEphemeralKey)
            return withContext(workContext) {
                val result = prefsRepository.setSavedSelection(paymentOption?.toSavedSelection())
                if (result) {
                    CustomerAdapter.Result.success(Unit)
                } else {
                    CustomerAdapter.Result.failure(
                        cause = IOException("Unable to persist payment option $paymentOption"),
                        displayMessage = context.getString(R.string.stripe_something_went_wrong)
                    )
                }
            }
        }
    }

    override suspend fun retrieveSelectedPaymentOption(): CustomerAdapter.Result<CustomerAdapter.PaymentOption?> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            val prefsRepository = prefsRepositoryFactory(customerEphemeralKey)
            val savedSelection = prefsRepository.getSavedSelection(
                /*
                 * We don't calculate on `Google Pay` availability in this function. Instead, we check
                 * within `CustomerSheet` similar to how we check if a saved payment option is still exists
                 * within the user's payment methods from `retrievePaymentMethods`
                 */
                isGooglePayAvailable = true,
                isLinkAvailable = false,
            )
            savedSelection.toPaymentOption()
        }
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): CustomerAdapter.Result<String> {
        if (setupIntentClientSecretProvider == null) {
            throw IllegalArgumentException("setupIntentClientSecretProvider cannot be null")
        }
        return getCustomerEphemeralKey().flatMap { customerEphemeralKey ->
            setupIntentClientSecretProvider.provideSetupIntentClientSecret(customerEphemeralKey.customerId)
        }
    }

    internal suspend fun getCustomerEphemeralKey(): CustomerAdapter.Result<CustomerEphemeralKey> {
        return withContext(workContext) {
            cachedCustomerEphemeralKey.takeUnless { cachedCustomerEphemeralKey ->
                cachedCustomerEphemeralKey == null || shouldRefreshCustomer(
                    cachedCustomerEphemeralKey.date
                )
            }?.result ?: run {
                val newCachedCustomerEphemeralKey = CachedCustomerEphemeralKey(
                    result = customerEphemeralKeyCoalescingOrchestrator.get(),
                    date = timeProvider(),
                )
                cachedCustomerEphemeralKey = newCachedCustomerEphemeralKey
                newCachedCustomerEphemeralKey.result
            }
        }
    }

    private fun shouldRefreshCustomer(cacheDate: Long): Boolean {
        val nowInMillis = timeProvider()
        return cacheDate + CACHED_CUSTOMER_MAX_AGE_MILLIS < nowInMillis
    }

    internal companion object {
        // 30 minutes, server-side timeout is 60
        internal const val CACHED_CUSTOMER_MAX_AGE_MILLIS = 60 * 30 * 1000L

        private val supportedPaymentMethodTypes: List<PaymentMethod.Type> = listOf(
            PaymentMethod.Type.Card,
            PaymentMethod.Type.USBankAccount,
        )
    }
}

private data class CachedCustomerEphemeralKey(
    val result: CustomerAdapter.Result<CustomerEphemeralKey>,
    val date: Long,
)
