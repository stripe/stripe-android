package com.stripe.android.customersheet.data

import com.stripe.android.common.validation.CustomerSessionClientSecretValidator
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.model.ElementsSession
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PrefsRepository
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.withContext
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

internal interface CustomerSessionElementsSessionManager {
    suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey>

    suspend fun fetchElementsSession(): Result<CustomerSessionElementsSession>
}

internal data class CustomerSessionElementsSession(
    val elementsSession: ElementsSession,
    val customer: ElementsSession.Customer,
    val ephemeralKey: CachedCustomerEphemeralKey,
)

@OptIn(ExperimentalCustomerSessionApi::class)
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
    private var cachedCustomerEphemeralKey: CachedCustomerEphemeralKey? = null

    private var intentConfiguration: CustomerSheet.IntentConfiguration? = null

    override suspend fun fetchCustomerSessionEphemeralKey(): Result<CachedCustomerEphemeralKey> {
        return withContext(workContext) {
            runCatching {
                cachedCustomerEphemeralKey.takeUnless { cachedCustomerEphemeralKey ->
                    cachedCustomerEphemeralKey == null ||
                        cachedCustomerEphemeralKey.shouldRefresh(timeProvider())
                } ?: fetchElementsSession().getOrThrow().ephemeralKey
            }
        }
    }

    override suspend fun fetchElementsSession(): Result<CustomerSessionElementsSession> {
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
                    initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                        intentConfiguration = PaymentSheet.IntentConfiguration(
                            mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                            paymentMethodTypes = intentConfiguration.paymentMethodTypes,
                        )
                    ),
                    savedPaymentMethodSelectionId = savedSelection?.id,
                    customer = PaymentSheet.CustomerConfiguration.createWithCustomerSession(
                        id = customerSessionClientSecret.customerId,
                        clientSecret = customerSessionClientSecret.clientSecret,
                    ),
                    customPaymentMethods = listOf(),
                    externalPaymentMethods = listOf(),
                ).onSuccess {
                    reportSuccessfulElementsSessionLoad()
                }.onFailure {
                    reportFailedElementsSessionLoad(it)
                }.mapCatching { elementsSession ->
                    createCustomerSessionElementsSession(elementsSession, customerSessionClientSecret.clientSecret)
                }.onSuccess { customerSessionElementsSession ->
                    cachedCustomerEphemeralKey = customerSessionElementsSession.ephemeralKey
                }.getOrThrow()
            }
        }
    }

    private fun createCustomerSessionElementsSession(
        elementsSession: ElementsSession,
        customerSessionClientSecret: String,
    ): CustomerSessionElementsSession {
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

        val customerSession = customer.session

        return CustomerSessionElementsSession(
            elementsSession = elementsSession,
            customer = customer,
            ephemeralKey = CachedCustomerEphemeralKey(
                customerId = customerSession.customerId,
                customerSessionClientSecret = customerSessionClientSecret,
                ephemeralKey = customerSession.apiKey,
                expiresAt = customerSession.apiKeyExpiry,
            )
        )
    }

    private fun reportSuccessfulElementsSessionLoad() {
        errorReporter.report(
            errorEvent = ErrorReporter
                .SuccessEvent
                .CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_SUCCESS,
        )
    }

    private fun reportFailedElementsSessionLoad(cause: Throwable) {
        errorReporter.report(
            errorEvent = ErrorReporter
                .ExpectedErrorEvent
                .CUSTOMER_SHEET_CUSTOMER_SESSION_ELEMENTS_SESSION_LOAD_FAILURE,
            stripeException = StripeException.create(cause)
        )
    }

    private fun validateCustomerSessionClientSecret(customerSessionClientSecret: String) {
        val result = CustomerSessionClientSecretValidator
            .validate(customerSessionClientSecret)

        val error = when (result) {
            is CustomerSessionClientSecretValidator.Result.Error.Empty -> {
                "The provided 'customerSessionClientSecret' cannot be an empty string."
            }
            is CustomerSessionClientSecretValidator.Result.Error.LegacyEphemeralKey -> {
                "Provided secret looks like an Ephemeral Key secret, but expecting a CustomerSession client " +
                    "secret. See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
            }
            is CustomerSessionClientSecretValidator.Result.Error.UnknownKey -> {
                "Provided secret does not look like a CustomerSession client secret. " +
                    "See CustomerSession API: https://docs.stripe.com/api/customer_sessions/create"
            }
            is CustomerSessionClientSecretValidator.Result.Valid -> null
        }

        error?.let {
            throw IllegalArgumentException(error)
        }
    }
}
