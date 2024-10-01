package com.stripe.android.customersheet.data

import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface CustomerSessionElementsSessionManager {
    suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey.Available>

    suspend fun fetchElementsSession(): Result<ElementsSessionWithCustomer>
}

internal data class ElementsSessionWithCustomer(
    val elementsSession: ElementsSession,
    val customer: ElementsSession.Customer,
)

@OptIn(ExperimentalCustomerSheetApi::class, ExperimentalCustomerSessionApi::class)
@Singleton
internal class DefaultCustomerSessionElementsSessionManager @Inject constructor(
    private val elementsSessionRepository: ElementsSessionRepository,
    private val prefsRepositoryFactory: @JvmSuppressWildcards (String) -> PrefsRepository,
    private val customerSessionProvider: CustomerSheet.CustomerSessionProvider,
    private val errorReporter: ErrorReporter,
    private val timeProvider: () -> Long,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSessionElementsSessionManager {
    @Volatile
    private var cachedCustomerEphemeralKey: CachedCustomerEphemeralKey = CachedCustomerEphemeralKey.None

    private var intentConfiguration: CustomerSheet.IntentConfiguration? = null

    override suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey.Available> {
        return withContext(workContext) {
            runCatching {
                val ephemeralKey = cachedCustomerEphemeralKey.takeUnless { cachedCustomerEphemeralKey ->
                    cachedCustomerEphemeralKey.shouldRefresh(timeProvider())
                } ?: run {
                    fetchElementsSession().getOrThrow()

                    cachedCustomerEphemeralKey
                }

                when (ephemeralKey) {
                    is CachedCustomerEphemeralKey.Available -> ephemeralKey
                    is CachedCustomerEphemeralKey.None -> throw IllegalStateException(
                        "No ephemeral key available!"
                    )
                }
            }
        }
    }

    override suspend fun fetchElementsSession(): Result<ElementsSessionWithCustomer> {
        return withContext(workContext) {
            runCatching {
                val intentConfiguration = intentConfiguration
                    ?: customerSessionProvider.intentConfiguration()
                        .onSuccess { intentConfiguration = it }
                        .getOrThrow()

                val customerSessionClientSecret = customerSessionProvider
                    .providesCustomerSessionClientSecret()
                    .getOrThrow()

                validateCustomerSessionClientSecret(customerSessionClientSecret.clientSecret)

                val prefsRepository = prefsRepositoryFactory(customerSessionClientSecret.customerId)

                val savedSelection = prefsRepository.getSavedSelection(
                    isGooglePayAvailable = false,
                    isLinkAvailable = false,
                ) as? SavedSelection.PaymentMethod

                elementsSessionRepository.get(
                    initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                        intentConfiguration = PaymentSheet.IntentConfiguration(
                            mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                            paymentMethodTypes = intentConfiguration.paymentMethodTypes,
                        )
                    ),
                    defaultPaymentMethodId = savedSelection?.id,
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = customerSessionClientSecret.customerId,
                        clientSecret = customerSessionClientSecret.clientSecret,
                    ),
                    externalPaymentMethods = listOf(),
                ).mapCatching { elementsSession ->
                    val customer = elementsSession.customer ?: run {
                        errorReporter.report(
                            ErrorReporter
                                .UnexpectedErrorEvent
                                .CUSTOMER_SESSION_ON_CUSTOMER_SHEET_ELEMENTS_SESSION_NO_CUSTOMER_FIELD
                        )

                        throw IllegalStateException(
                            "`customer` field should be available when using `CustomerSession` in elements/session!"
                        )
                    }

                    ElementsSessionWithCustomer(
                        elementsSession = elementsSession,
                        customer = customer
                    )
                }.onSuccess { elementsSessionWithCustomer ->
                    val customerSession = elementsSessionWithCustomer.customer.session

                    cachedCustomerEphemeralKey = CachedCustomerEphemeralKey.Available(
                        customerId = customerSession.customerId,
                        ephemeralKey = customerSession.apiKey,
                        expiresAt = customerSession.apiKeyExpiry,
                    )
                }.getOrThrow()
            }
        }
    }

    private fun validateCustomerSessionClientSecret(customerSessionClientSecret: String) {
        val error = when {
            customerSessionClientSecret.isBlank() -> {
                "The 'customerSessionClientSecret' cannot be an empty string."
            }
            customerSessionClientSecret.startsWith(EPHEMERAL_KEY_SECRET_PREFIX) -> {
                "Provided secret looks like an Ephemeral Key secret, but expecting a CustomerSession client " +
                    "secret. See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
            }
            !customerSessionClientSecret.startsWith(CUSTOMER_SESSION_CLIENT_SECRET_KEY_PREFIX) -> {
                "Provided secret does not look like a CustomerSession client secret. " +
                    "See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
            }
            else -> null
        }

        error?.let {
            throw IllegalArgumentException(it)
        }
    }

    private companion object {
        const val EPHEMERAL_KEY_SECRET_PREFIX = "ek_"
        const val CUSTOMER_SESSION_CLIENT_SECRET_KEY_PREFIX = "cuss_"
    }
}
