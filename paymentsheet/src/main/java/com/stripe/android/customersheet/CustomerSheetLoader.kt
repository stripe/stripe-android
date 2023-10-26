package com.stripe.android.customersheet

import com.stripe.android.core.injection.IS_LIVE_MODE
import com.stripe.android.googlepaylauncher.GooglePayEnvironment
import com.stripe.android.googlepaylauncher.GooglePayRepository
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.requireValidOrThrow
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.toInternal
import com.stripe.android.ui.core.BillingDetailsCollectionConfiguration
import com.stripe.android.ui.core.forms.resources.LpmRepository
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
        return elementsSessionRepository.get(
            PaymentSheet.InitializationMode.DeferredIntent(
                PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                    paymentMethodTypes = listOf(
                        PaymentMethod.Type.Card.code,
                        PaymentMethod.Type.USBankAccount.code,
                    )
                )
            )
        ).mapCatching { elementsSession ->
            val billingDetailsCollectionConfig =
                configuration?.billingDetailsCollectionConfiguration?.toInternal()
                    ?: BillingDetailsCollectionConfiguration()

            lpmRepository.update(
                stripeIntent = elementsSession.stripeIntent,
                serverLpmSpecs = elementsSession.paymentMethodSpecs,
                billingDetailsCollectionConfiguration = billingDetailsCollectionConfig,
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

                Result.success(
                    CustomerSheetState.Full(
                        config = configuration,
                        stripeIntent = elementsSession?.stripeIntent,
                        customerPaymentMethods = paymentMethods,
                        isGooglePayReady = isGooglePayReadyAndEnabled,
                        paymentSelection = paymentSelection
                    )
                )
            },
            onFailure = { cause, _ ->
                Result.failure(cause)
            }
        )
    }
}
