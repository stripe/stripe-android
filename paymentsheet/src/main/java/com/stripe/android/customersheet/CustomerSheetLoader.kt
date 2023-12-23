package com.stripe.android.customersheet

import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.payments.financialconnections.IsFinancialConnectionsAvailable
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.requireValidOrThrow
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.toInternal
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.ui.core.forms.resources.LpmRepository
import com.stripe.android.utils.FeatureFlags.customerSheetACHv2
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named

@OptIn(ExperimentalCustomerSheetApi::class)
internal interface CustomerSheetLoader {
    suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState.Full>
}

@OptIn(ExperimentalCustomerSheetApi::class)
internal class DefaultCustomerSheetLoader @Inject constructor(
    @Named(IS_LIVE_MODE) private val isLiveModeProvider: () -> Boolean,
    private val googlePayRepositoryFactory: @JvmSuppressWildcards (GooglePayEnvironment) -> GooglePayRepository,
    private val elementsSessionRepository: ElementsSessionRepository,
    private val isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    private val lpmRepository: LpmRepository,
    private val customerAdapter: CustomerAdapter,
) : CustomerSheetLoader {
    override suspend fun load(configuration: CustomerSheet.Configuration?): Result<CustomerSheetState.Full> {
        val elementsSession = if (customerAdapter.canCreateSetupIntents) {
            retrieveElementsSession(configuration).getOrElse {
                return Result.failure(it)
            }
        } else {
            null
        }

        return loadPaymentMethods(
            configuration = configuration,
            elementsSession = elementsSession,
        )
    }

    private suspend fun retrieveElementsSession(
        configuration: CustomerSheet.Configuration?,
    ): Result<ElementsSession> {
        val initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
            )
        )
        return elementsSessionRepository.get(initializationMode).mapCatching { elementsSession ->
            val billingDetailsCollectionConfig = configuration?.billingDetailsCollectionConfiguration.toInternal()

            lpmRepository.update(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
                isDeferred = true,
            )

            elementsSession.requireValidOrThrow()
        }
    }

    private suspend fun loadPaymentMethods(
        configuration: CustomerSheet.Configuration?,
        elementsSession: ElementsSession?,
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

                // By default, only cards are supported. If the elements session is not available, then US Bank account
                // is not supported
                val supportedPaymentMethods = elementsSession?.stripeIntent?.paymentMethodTypes?.mapNotNull {
                    lpmRepository.fromCode(it)
                } ?: listOf(LpmRepository.hardcodedCardSpec(billingDetailsCollectionConfig))

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
                    )
                )
            },
            onFailure = { cause, _ ->
                Result.failure(cause)
            }
        )
    }

    private fun filterSupportedPaymentMethods(
        supportedPaymentMethods: List<LpmRepository.SupportedPaymentMethod>,
        isFinancialConnectionsAvailable: IsFinancialConnectionsAvailable,
    ): List<LpmRepository.SupportedPaymentMethod> {
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
