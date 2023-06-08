package com.stripe.android.customersheet

import android.content.Context
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toSavedSelection
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.PaymentOption
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.IllegalArgumentException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * The default implementation of [CustomerAdapter]. This adapter uses the customer ID and ephemeral
 * key provided by [CustomerEphemeralKeyProvider] to read, update, or delete the customer's
 * default saved payment method using Android [SharedPreferences]. By default, this adapter saves
 * the customer's selected payment method to [SharedPreferences], which is used by [PaymentSheet]
 * to load the customer's default saved payment method.
 */
@OptIn(ExperimentalCustomerSheetApi::class)
@Suppress("unused")
@JvmSuppressWildcards
internal class StripeCustomerAdapter @Inject constructor(
    private val context: Context,
    private val customerEphemeralKeyProvider: CustomerEphemeralKeyProvider,
    private val setupIntentClientSecretProvider: SetupIntentClientSecretProvider?,
    private val timeProvider: () -> Long,
    private val customerRepository: CustomerRepository,
    private val prefsRepositoryFactory: (CustomerEphemeralKey) -> PrefsRepository,
    @IOContext private val workContext: CoroutineContext,
) : CustomerAdapter {

    @Volatile
    private var cachedCustomerEphemeralKey: CachedCustomerEphemeralKey? = null

    override suspend fun retrievePaymentMethods(): Result<List<PaymentMethod>> {
        return getCustomerEphemeralKey().map { customerEphemeralKey ->
            val paymentMethods = customerRepository.getPaymentMethods(
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey
                ),
                types = listOf(PaymentMethod.Type.Card)
            )
            paymentMethods
        }
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): Result<PaymentMethod> {
        return getCustomerEphemeralKey().map { customerEphemeralKey ->
            customerRepository.attachPaymentMethod(
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey
                ),
                paymentMethodId = paymentMethodId
            ).getOrElse {
                return Result.failure(it)
            }
        }
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): Result<PaymentMethod> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            customerRepository.detachPaymentMethod(
                customerConfig = PaymentSheet.CustomerConfiguration(
                    id = customerEphemeralKey.customerId,
                    ephemeralKeySecret = customerEphemeralKey.ephemeralKey
                ),
                paymentMethodId = paymentMethodId
            ).getOrElse {
                return Result.failure(it)
            }
        }
    }

    override suspend fun setSelectedPaymentOption(
        paymentOption: CustomerAdapter.PaymentOption?
    ): Result<CustomerAdapter.PaymentOption?> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            val prefsRepository = prefsRepositoryFactory(customerEphemeralKey)
            withContext(workContext) {
                val result = prefsRepository.setSavedSelection(paymentOption?.toSavedSelection())
                if (result) {
                    Result.success(paymentOption)
                } else {
                    Result.failure(
                        IOException("Unable to set the payment option: $paymentOption")
                    )
                }
            }
        }.getOrElse {
            Result.failure(it)
        }
    }

    override suspend fun retrieveSelectedPaymentOption(): Result<CustomerAdapter.PaymentOption?> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            val prefsRepository = prefsRepositoryFactory(customerEphemeralKey)
            val savedSelection = prefsRepository.getSavedSelection(
                isGooglePayAvailable = false,
                isLinkAvailable = false,
            )
            savedSelection.toPaymentOption()
        }
    }

    override suspend fun setupIntentClientSecretForCustomerAttach(): Result<String> {
        return getCustomerEphemeralKey().mapCatching { customerEphemeralKey ->
            setupIntentClientSecretProvider?.provide(customerEphemeralKey.customerId)
        }.getOrElse {
            Result.failure(it)
        } ?: throw IllegalArgumentException("setupIntentClientSecretProvider cannot be null")
    }

    internal suspend fun getCustomerEphemeralKey(): Result<CustomerEphemeralKey> {
        return withContext(workContext) {
            cachedCustomerEphemeralKey.takeUnless { cachedCustomerEphemeralKey ->
                cachedCustomerEphemeralKey == null || shouldRefreshCustomer(
                    cachedCustomerEphemeralKey.date
                )
            }?.result ?: run {
                val newCachedCustomerEphemeralKey = CachedCustomerEphemeralKey(
                    result = customerEphemeralKeyProvider.provide(),
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
    }
}

@OptIn(ExperimentalCustomerSheetApi::class)
private data class CachedCustomerEphemeralKey(
    val result: Result<CustomerEphemeralKey>,
    val date: Long,
)
