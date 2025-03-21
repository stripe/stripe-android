package com.stripe.android.customersheet.data

import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.map
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
internal class CustomerAdapterDataSource @Inject constructor(
    private val elementsSessionRepository: ElementsSessionRepository,
    private val customerAdapter: CustomerAdapter,
    private val errorReporter: ErrorReporter,
    @IOContext private val workContext: CoroutineContext,
) : CustomerSheetInitializationDataSource,
    CustomerSheetSavedSelectionDataSource,
    CustomerSheetPaymentMethodDataSource,
    CustomerSheetIntentDataSource {
    override val canCreateSetupIntents: Boolean = customerAdapter.canCreateSetupIntents

    override suspend fun loadCustomerSheetSession(
        configuration: CustomerSheet.Configuration,
    ): CustomerSheetDataResult<CustomerSheetSession> {
        return workContext.runCatching {
            val elementsSessionResult = async {
                fetchElementsSession()
            }

            val paymentMethodsResult = async {
                fetchInitialPaymentMethods()
            }

            val savedSelectionResult = async {
                retrieveSavedSelection(customerSessionElementsSession = null).toResult()
            }

            val elementsSession = elementsSessionResult.await().getOrThrow()
            val paymentMethods = paymentMethodsResult.await().getOrThrow()
            val savedSelection = savedSelectionResult.await().getOrThrow()

            CustomerSheetSession(
                elementsSession = elementsSession,
                paymentMethods = paymentMethods,
                // Always `Legacy` for adapter use case
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
                savedSelection = savedSelection,
                permissions = CustomerPermissions(
                    canRemoveLastPaymentMethod = configuration.allowsRemovalOfLastSavedPaymentMethod,
                    // Always `true` for `Adapter` use case
                    canRemovePaymentMethods = true,
                ),
                // Default payment methods are a customer sessions-only feature, so this value is unused.
                defaultPaymentMethodId = null,
            )
        }.toCustomerSheetDataResult()
    }

    override suspend fun retrievePaymentMethods() = runCatchingAdapterTask {
        customerAdapter.retrievePaymentMethods()
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ) = runCatchingAdapterTask {
        customerAdapter.updatePaymentMethod(paymentMethodId, params)
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String) = runCatchingAdapterTask {
        customerAdapter.attachPaymentMethod(paymentMethodId)
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String) = runCatchingAdapterTask {
        customerAdapter.detachPaymentMethod(paymentMethodId)
    }

    override suspend fun retrieveSavedSelection(
        customerSessionElementsSession: CustomerSessionElementsSession?
    ) = runCatchingAdapterTask {
        customerAdapter.retrieveSelectedPaymentOption().map { result ->
            result?.toSavedSelection()
        }
    }

    override suspend fun setSavedSelection(
        selection: SavedSelection?,
        shouldSyncDefault: Boolean
    ) = runCatchingAdapterTask {
        customerAdapter.setSelectedPaymentOption(selection?.toPaymentOption())
    }

    override suspend fun retrieveSetupIntentClientSecret() = runCatchingAdapterTask {
        customerAdapter.setupIntentClientSecretForCustomerAttach()
    }

    private suspend fun fetchElementsSession(): Result<ElementsSession> {
        val paymentMethodTypes = createPaymentMethodTypes()
        val initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
            PaymentSheet.IntentConfiguration(
                mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                paymentMethodTypes = paymentMethodTypes,
            )
        )

        return elementsSessionRepository.get(
            initializationMode,
            customer = null,
            externalPaymentMethods = emptyList(),
            customPaymentMethods = listOf(),
            savedPaymentMethodSelectionId = null,
        ).onSuccess {
            errorReporter.report(
                errorEvent = ErrorReporter.SuccessEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_SUCCESS,
            )
        }.onFailure {
            errorReporter.report(
                errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_FAILURE,
                stripeException = StripeException.create(it)
            )
        }
    }

    private suspend fun fetchInitialPaymentMethods(): Result<List<PaymentMethod>> {
        return retrievePaymentMethods()
            .onSuccess {
                errorReporter.report(
                    errorEvent = ErrorReporter.SuccessEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_SUCCESS,
                )
            }
            .onFailure { cause, _ ->
                errorReporter.report(
                    errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE,
                    stripeException = StripeException.create(cause)
                )
            }
            .toResult()
    }

    private fun createPaymentMethodTypes(): List<String> {
        return if (customerAdapter.canCreateSetupIntents) {
            customerAdapter.paymentMethodTypes ?: emptyList()
        } else {
            // We only support cards if `customerAdapter.canCreateSetupIntents` is false.
            listOf("card")
        }
    }

    private suspend fun <T> runCatchingAdapterTask(
        task: suspend () -> CustomerAdapter.Result<T>
    ): CustomerSheetDataResult<T> = withContext(workContext) {
        runCatching {
            task()
        }.fold(
            onSuccess = { it.toCustomerSheetDataResult() },
            onFailure = {
                CustomerSheetDataResult.failure(cause = it, displayMessage = null)
            }
        )
    }
}
