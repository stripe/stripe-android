package com.stripe.android.customersheet

import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.customersheet.util.CustomerSheetHacks
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
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.address.AddressRepository
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Named
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
    private val addressRepository: AddressRepository,
    private val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    private val lpmRepository: LpmRepository,
    private val customerAdapterProvider: Deferred<CustomerAdapter>,
    private val errorReporter: ErrorReporter,
) : CustomerSheetLoader {

    @Inject constructor(
        @Named(IS_LIVE_MODE) isLiveModeProvider: () -> Boolean,
        googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
        elementsSessionRepository: ElementsSessionRepository,
        addressRepository: AddressRepository,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
        lpmRepository: LpmRepository,
        errorReporter: ErrorReporter,
    ) : this(
        isLiveModeProvider = isLiveModeProvider,
        googlePayRepositoryFactory = googlePayRepositoryFactory,
        elementsSessionRepository = elementsSessionRepository,
        addressRepository = addressRepository,
        isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
        lpmRepository = lpmRepository,
        customerAdapterProvider = CustomerSheetHacks.adapter,
        errorReporter = errorReporter,
    )

    override suspend fun load(configuration: CustomerSheet.Configuration): Result<CustomerSheetState.Full> {
        return runCatching {
            val customerAdapter = customerAdapterProvider.awaitAsResult(
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
            }.getOrThrow()

            val elementsSessionWithMetadata = retrieveElementsSession(
                configuration = configuration,
                customerAdapter = customerAdapter
            ).onFailure {
                errorReporter.report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_FAILURE,
                    stripeException = StripeException.create(it)
                )
            }.getOrThrow()

            loadPaymentMethods(
                customerAdapter = customerAdapter,
                configuration = configuration,
                elementsSessionWithMetadata = elementsSessionWithMetadata,
            ).onFailure {
                errorReporter.report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE,
                    stripeException = StripeException.create(it)
                )
            }.getOrThrow()
        }
    }

    private suspend fun retrieveElementsSession(
        configuration: CustomerSheet.Configuration,
        customerAdapter: CustomerAdapter,
    ): Result<ElementsSessionWithMetadata> {
        val paymentMethodTypes = if (customerAdapter.canCreateSetupIntents) {
            customerAdapter.paymentMethodTypes ?: emptyList()
        } else {
            // We only support cards if `customerAdapter.canCreateSetupIntents` is false.
            listOf("card")
        }
        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                paymentMethodTypes = paymentMethodTypes,
            )
        )
        return elementsSessionRepository.get(initializationMode, externalPaymentMethods = null).map { elementsSession ->
            val billingDetailsCollectionConfig = configuration.billingDetailsCollectionConfiguration
            val sharedDataSpecs = lpmRepository.getSharedDataSpecs(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
            ).sharedDataSpecs

            val cbcEligibility = CardBrandChoiceEligibility.create(
                isEligible = elementsSession.isEligibleForCardBrandChoice,
                preferredNetworks = configuration.preferredNetworks,
            )

            val metadata = PaymentMethodMetadata(
                stripeIntent = elementsSession.stripeIntent,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
                allowsDelayedPaymentMethods = true,
                allowsPaymentMethodsRequiringShippingAddress = false,
                paymentMethodOrder = configuration.paymentMethodOrder,
                cbcEligibility = cbcEligibility,
                merchantName = configuration.merchantDisplayName,
                defaultBillingDetails = configuration.defaultBillingDetails,
                shippingDetails = null,
                hasCustomerConfiguration = true,
                sharedDataSpecs = sharedDataSpecs,
                addressSchemas = addressRepository.load(),
                financialConnectionsAvailable = isFinancialConnectionsAvailable()
            )

            ElementsSessionWithMetadata(elementsSession = elementsSession, metadata = metadata)
        }
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
                var paymentMethods = result.first
                val paymentSelection = result.second

                paymentSelection?.apply {
                    val selectedPaymentMethod = (this as? PaymentSelection.Saved)?.paymentMethod
                    // The order of the payment methods should be selected PM and then any additional PMs
                    // The carousel always starts with Add and Google Pay (if enabled)
                    paymentMethods = paymentMethods.sortedWith { left, right ->
                        // We only care to move the selected payment method, all others stay in the
                        // order they were before
                        when {
                            left.id == selectedPaymentMethod?.id -> -1
                            right.id == selectedPaymentMethod?.id -> 1
                            else -> 0
                        }
                    }
                }

                val isGooglePayReadyAndEnabled = configuration.googlePayEnabled && googlePayRepositoryFactory(
                    if (isLiveModeProvider()) GooglePayEnvironment.Production else GooglePayEnvironment.Test
                ).isReady().first()

                val elementsSession = elementsSessionWithMetadata.elementsSession
                val metadata = elementsSessionWithMetadata.metadata

                val supportedPaymentMethods = metadata.sortedSupportedPaymentMethods()

                val validSupportedPaymentMethods = filterSupportedPaymentMethods(supportedPaymentMethods)

                Result.success(
                    CustomerSheetState.Full(
                        config = configuration,
                        paymentMethodMetadata = metadata,
                        supportedPaymentMethods = validSupportedPaymentMethods,
                        customerPaymentMethods = paymentMethods,
                        isGooglePayReady = isGooglePayReadyAndEnabled,
                        paymentSelection = paymentSelection,
                        validationError = elementsSession.stripeIntent.validate(),
                    )
                )
            },
            onFailure = { cause, _ ->
                Result.failure(cause)
            }
        )
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
