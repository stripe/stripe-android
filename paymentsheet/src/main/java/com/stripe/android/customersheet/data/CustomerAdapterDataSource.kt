package com.stripe.android.customersheet.data

import com.stripe.android.common.coroutines.runCatching
import com.stripe.android.core.exception.StripeException
import com.stripe.android.core.injection.IOContext
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerAdapter.PaymentOption.Companion.toPaymentOption
import com.stripe.android.customersheet.CustomerPermissions
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.map
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.ElementsSession
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCustomerSheetApi::class)
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

    override suspend fun loadCustomerSheetSession(): CustomerSheetDataResult<CustomerSheetSession> {
        return workContext.runCatching {
            val elementsSessionResult = async {
                fetchElementsSession()
            }

            val paymentMethodsResult = async {
                fetchInitialPaymentMethods()
            }

            val savedSelectionResult = async {
                retrieveSavedSelection().toResult()
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
                    // Always `true` for `Adapter` use case
                    canRemovePaymentMethods = true,
                )
            )
        }.toCustomerSheetDataResult()
    }

    override suspend fun retrievePaymentMethods(): CustomerSheetDataResult<List<PaymentMethod>> {
        return customerAdapter.retrievePaymentMethods().toCustomerSheetDataResult()
    }

    override suspend fun updatePaymentMethod(
        paymentMethodId: String,
        params: PaymentMethodUpdateParams,
    ): CustomerSheetDataResult<PaymentMethod> {
        return customerAdapter.updatePaymentMethod(paymentMethodId, params).toCustomerSheetDataResult()
    }

    override suspend fun attachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return customerAdapter.attachPaymentMethod(paymentMethodId).toCustomerSheetDataResult()
    }

    override suspend fun detachPaymentMethod(paymentMethodId: String): CustomerSheetDataResult<PaymentMethod> {
        return customerAdapter.detachPaymentMethod(paymentMethodId).toCustomerSheetDataResult()
    }

    override suspend fun retrieveSavedSelection(): CustomerSheetDataResult<SavedSelection?> {
        return customerAdapter.retrieveSelectedPaymentOption().map { result ->
            result?.toSavedSelection()
        }.toCustomerSheetDataResult()
    }

    override suspend fun setSavedSelection(selection: SavedSelection?): CustomerSheetDataResult<Unit> {
        return customerAdapter.setSelectedPaymentOption(selection?.toPaymentOption()).toCustomerSheetDataResult()
    }

    override suspend fun retrieveSetupIntentClientSecret(): CustomerSheetDataResult<String> {
        return customerAdapter.setupIntentClientSecretForCustomerAttach().toCustomerSheetDataResult()
    }

    private suspend fun fetchElementsSession(): Result<ElementsSession> {
        val paymentMethodTypes = createPaymentMethodTypes()
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

    private suspend fun fetchInitialPaymentMethods(): Result<List<PaymentMethod>> {
        return retrievePaymentMethods().toResult().onFailure {
            errorReporter.report(
                errorEvent = ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE,
                stripeException = StripeException.create(it)
            )
        }
    }

    private fun createPaymentMethodTypes(): List<String> {
        return if (customerAdapter.canCreateSetupIntents) {
            customerAdapter.paymentMethodTypes ?: emptyList()
        } else {
            // We only support cards if `customerAdapter.canCreateSetupIntents` is false.
            listOf("card")
        }
    }
}
