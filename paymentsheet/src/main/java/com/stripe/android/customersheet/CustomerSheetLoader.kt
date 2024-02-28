package com.stripe.android.customersheet

import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.core.utils.FeatureFlags.customerSheetACHv2
import com.stripe.android.customersheet.util.CustomerSheetHacks
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.lpmfoundations.luxe.LpmRepository
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.validate
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.toInternal
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
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
    suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState.Full>
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal class DefaultCustomerSheetLoader(
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    private val lpmRepository: LpmRepository,
    private val customerAdapterProvider: Deferred<CustomerAdapter>,
) : CustomerSheetLoader {

    @Inject constructor(
        @Named(IS_LIVE_MODE) isLiveModeProvider: () -> Boolean,
        googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
        elementsSessionRepository: ElementsSessionRepository,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
        lpmRepository: LpmRepository,
    ) : this(
        isLiveModeProvider = isLiveModeProvider,
        googlePayRepositoryFactory = googlePayRepositoryFactory,
        elementsSessionRepository = elementsSessionRepository,
        isFinancialConnectionsAvailable = isFinancialConnectionsAvailable,
        lpmRepository = lpmRepository,
        customerAdapterProvider = CustomerSheetHacks.adapter,
    )

    override suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState.Full> {
        val customerAdapter = customerAdapterProvider.awaitAsResult(
            timeout = 5.seconds,
            error = {
                "Couldn't find an instance of CustomerAdapter. " +
                    "Are you instantiating CustomerSheet unconditionally in your app?"
            },
        )

        return customerAdapter.mapCatching { adapter ->
            if (adapter.canCreateSetupIntents) {
                adapter to retrieveElementsSession(configuration, adapter).getOrThrow()
            } else {
                adapter to null
            }
        }.map { (adapter, elementsSessionWithMetadata) ->
            loadPaymentMethods(
                customerAdapter = adapter,
                configuration = configuration,
                elementsSessionWithMetadata = elementsSessionWithMetadata,
            ).getOrThrow()
        }
    }

    private suspend fun retrieveElementsSession(
        configuration: CustomerSheet.Configuration?,
        customerAdapter: CustomerAdapter,
    ): Result<ElementsSessionWithMetadata> {
        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                paymentMethodTypes = customerAdapter.paymentMethodTypes ?: emptyList()
            )
        )
        return elementsSessionRepository.get(initializationMode).map { elementsSession ->
            val billingDetailsCollectionConfig = configuration?.billingDetailsCollectionConfiguration.toInternal()
            val sharedDataSpecs = lpmRepository.getSharedDataSpecs(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
            ).sharedDataSpecs
            val metadata = PaymentMethodMetadata(
                stripeIntent = elementsSession.stripeIntent,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
                allowsDelayedPaymentMethods = true,
                allowsPaymentMethodsRequiringShippingAddress = false,
                sharedDataSpecs = sharedDataSpecs,
                financialConnectionsAvailable = isFinancialConnectionsAvailable()
            )

            lpmRepository.update(
                metadata = metadata,
                specs = sharedDataSpecs,
            )
            ElementsSessionWithMetadata(elementsSession = elementsSession, metadata = metadata)
        }
    }

    private suspend fun loadPaymentMethods(
        customerAdapter: CustomerAdapter,
        configuration: CustomerSheet.Configuration?,
        elementsSessionWithMetadata: ElementsSessionWithMetadata?,
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

                val isGooglePayReadyAndEnabled = configuration?.googlePayEnabled == true && googlePayRepositoryFactory(
                    if (isLiveModeProvider()) GooglePayEnvironment.Production else GooglePayEnvironment.Test
                ).isReady().first()

                val billingDetailsCollectionConfig = configuration?.billingDetailsCollectionConfiguration.toInternal()

                val elementsSession = elementsSessionWithMetadata?.elementsSession
                val metadata = elementsSessionWithMetadata?.metadata

                // By default, only cards are supported. If the elements session is not available, then US Bank account
                // is not supported
                val supportedPaymentMethods = metadata?.sortedSupportedPaymentMethods()
                    ?: listOf(CardDefinition.hardcodedCardSpec(billingDetailsCollectionConfig))

                val validSupportedPaymentMethods = filterSupportedPaymentMethods(
                    supportedPaymentMethods,
                    isFinancialConnectionsAvailable,
                )

                val isCbcEligible = elementsSession?.isEligibleForCardBrandChoice ?: false

                Result.success(
                    CustomerSheetState.Full(
                        config = configuration,
                        stripeIntent = elementsSession?.stripeIntent,
                        supportedPaymentMethods = validSupportedPaymentMethods,
                        customerPaymentMethods = paymentMethods,
                        isGooglePayReady = isGooglePayReadyAndEnabled,
                        paymentSelection = paymentSelection,
                        cbcEligibility = if (isCbcEligible) {
                            CardBrandChoiceEligibility.Eligible(
                                preferredNetworks = configuration?.preferredNetworks.orEmpty(),
                            )
                        } else {
                            CardBrandChoiceEligibility.Ineligible
                        },
                        validationError = elementsSession?.stripeIntent?.validate(),
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
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    ): List<SupportedPaymentMethod> {
        val supported = setOfNotNull(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.USBankAccount.code.takeIf {
                customerSheetACHv2.isEnabled && isFinancialConnectionsAvailable()
            }
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
