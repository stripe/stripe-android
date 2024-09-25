package com.stripe.android.customersheet

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.customersheet.util.sortPaymentMethods
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.validate
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCustomerSheetApi::class)
internal interface CustomerSheetLoader {
    suspend fun load(configuration: CustomerSheet.Configuration): Result<CustomerSheetState.Full>
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal class DefaultCustomerSheetLoader(
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    private val lpmRepository: LpmRepository,
    private val customerAdapterProvider: Deferred<CustomerAdapter>,
    private val errorReporter: ErrorReporter,
    private val workContext: CoroutineContext
) : CustomerSheetLoader {

    @Inject constructor(
        @Named(IS_LIVE_MODE) isLiveModeProvider: () -> Boolean,
        googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
        elementsSessionRepository: ElementsSessionRepository,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
        lpmRepository: LpmRepository,
        errorReporter: ErrorReporter,
        @IOContext workContext: CoroutineContext
    ) : this(
        isLiveModeProvider = isLiveModeProvider,
        googlePayRepositoryFactory = googlePayRepositoryFactory,
        elementsSessionRepository = elementsSessionRepository,
        isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
        lpmRepository = lpmRepository,
        customerAdapterProvider = CustomerSheetHacks.adapter,
        errorReporter = errorReporter,
        workContext = workContext,
    )

    override suspend fun load(
        configuration: CustomerSheet.Configuration
    ): Result<CustomerSheetState.Full> = workContext.runCatching {
        val customerAdapter = retrieveCustomerAdapter().getOrThrow()

        val elementsSession = retrieveElementsSession(
            customerAdapter = customerAdapter,
        ).getOrThrow()

        val metadata = createPaymentMethodMetadata(
            configuration = configuration,
            elementsSession = elementsSession,
        )

        loadPaymentMethods(
            customerAdapter = customerAdapter,
            configuration = configuration,
            elementsSessionWithMetadata = ElementsSessionWithMetadata(
                elementsSession = elementsSession,
                metadata = metadata,
            ),
        ).onFailure {
            errorReporter.report(
                errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE,
                stripeException = StripeException.create(it)
            )
        }.getOrThrow()
    }

    private suspend fun retrieveCustomerAdapter(): Result<CustomerAdapter> {
        return customerAdapterProvider.awaitAsResult(
            timeout = 5.seconds,
            error = {
                "Couldn't find an instance of CustomerAdapter. " +
                    "Are you instantiating CustomerSheet unconditionally in your app?"
            },
        ).onFailure {
            errorReporter.report(
                errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ADAPTER_NOT_FOUND,
                stripeException = StripeException.create(it)
            )
        }
    }

    private suspend fun retrieveElementsSession(
        customerAdapter: CustomerAdapter,
    ): Result<ElementsSession> {
        val paymentMethodTypes = createPaymentMethodTypes(customerAdapter)
        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                paymentMethodTypes = paymentMethodTypes,
            )
        )
        return elementsSessionRepository.get(
            initializationMode,
            customer = null,
            externalPaymentMethods = emptyList(),
            defaultPaymentMethodId = null,
        ).onFailure {
            errorReporter.report(
                errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_FAILURE,
                stripeException = StripeException.create(it)
            )
        }
    }

    private suspend fun createPaymentMethodMetadata(
        configuration: CustomerSheet.Configuration,
        elementsSession: ElementsSession,
    ): PaymentMethodMetadata {
        val sharedDataSpecs = lpmRepository.getSharedDataSpecs(
            stripeIntent = elementsSession.stripeIntent,
            serverLpmSpecs = elementsSession.paymentMethodSpecs,
        ).sharedDataSpecs

        val isGooglePayReadyAndEnabled = configuration.googlePayEnabled && googlePayRepositoryFactory(
            if (isLiveModeProvider()) GooglePayEnvironment.Production else GooglePayEnvironment.Test
        ).isReady().first()

        return PaymentMethodMetadata.create(
            elementsSession = elementsSession,
            configuration = configuration,
            sharedDataSpecs = sharedDataSpecs,
            isGooglePayReady = isGooglePayReadyAndEnabled,
            isFinancialConnectionsAvailable = isFinancialConnectionsAvailable
        )
    }

    private suspend fun loadPaymentMethods(
        customerAdapter: CustomerAdapter,
        configuration: CustomerSheet.Configuration,
        elementsSessionWithMetadata: ElementsSessionWithMetadata,
    ) = coroutineScope {
        val paymentMethodsResult = async {
            customerAdapter.retrievePaymentMethods()
        }
        val selectedPaymentOption = async {
            customerAdapter.retrieveSelectedPaymentOption()
        }

        paymentMethodsResult.await().flatMap { paymentMethods ->
            selectedPaymentOption.await().map { paymentOption ->
                Pair(paymentMethods, paymentOption)
            }
        }.map {
            val paymentMethods = it.first
            val paymentOption = it.second
            val selection = paymentOption?.toPaymentSelection { id ->
                paymentMethods.find { it.id == id }
            }
            Pair(paymentMethods, selection)
        }.fold(
            onSuccess = { result ->
                val paymentSelection = result.second

                val sortedPaymentMethods = sortPaymentMethods(
                    paymentMethods = result.first,
                    selection = paymentSelection as? PaymentSelection.Saved
                )

                val elementsSession = elementsSessionWithMetadata.elementsSession
                val metadata = elementsSessionWithMetadata.metadata

                val supportedPaymentMethods = metadata.sortedSupportedPaymentMethods()

                val validSupportedPaymentMethods = filterSupportedPaymentMethods(supportedPaymentMethods)

                Result.success(
                    CustomerSheetState.Full(
                        config = configuration,
                        paymentMethodMetadata = metadata,
                        supportedPaymentMethods = validSupportedPaymentMethods,
                        customerPaymentMethods = sortedPaymentMethods,
                        paymentSelection = paymentSelection,
                        validationError = elementsSession.stripeIntent.validate(),
                        customerPermissions = CustomerPermissions(
                            // Should always be true for `legacy` case
                            canRemovePaymentMethods = true,
                        )
                    )
                )
            },
            onFailure = { cause, _ ->
                Result.failure(cause)
            }
        )
    }

    private fun createPaymentMethodTypes(
        customerAdapter: CustomerAdapter,
    ): List<String> {
        return if (customerAdapter.canCreateSetupIntents) {
            customerAdapter.paymentMethodTypes ?: emptyList()
        } else {
            // We only support cards if `customerAdapter.canCreateSetupIntents` is false.
            listOf("card")
        }
    }

    private fun filterSupportedPaymentMethods(
        supportedPaymentMethods: List<SupportedPaymentMethod>,
    ): List<SupportedPaymentMethod> {
        val supported = setOfNotNull(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.USBankAccount.code
        )
        return supportedPaymentMethods.filter {
            supported.contains(it.code)
        }
    }
}

private data class ElementsSessionWithMetadata(
    val elementsSession: ElementsSession,
    val metadata: PaymentMethodMetadata,
)

private suspend fun <T> Deferred<T>.awaitAsResult(
    timeout: Duration,
    error: () -> String,
): Result<T> {
    val result = withTimeoutOrNull(timeout) { await() }
    return if (result != null) {
        Result.success(result)
    } else {
        Result.failure(IllegalStateException(error()))
    }
}
