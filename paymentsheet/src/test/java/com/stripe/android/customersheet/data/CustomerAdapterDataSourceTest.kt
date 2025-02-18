package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.CustomerSheetFixtures
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.model.StripeIntent
import com.stripe.android.payments.core.analytics.ErrorReporter
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.paymentsheet.repositories.ElementsSessionRepository
import com.stripe.android.paymentsheet.state.PaymentElementLoader.InitializationMode
import com.stripe.android.testing.FakeErrorReporter
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.testing.SetupIntentFactory
import com.stripe.android.utils.FakeElementsSessionRepository
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test

class CustomerAdapterDataSourceTest {
    @Test
    fun `on retrieve payment methods, should complete successfully from adapter`() = runTest {
        val paymentMethods = PaymentMethodFactory.cards(size = 6)
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(paymentMethods),
            ),
        )

        val result = dataSource.retrievePaymentMethods()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<List<PaymentMethod>>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).containsExactlyElementsIn(paymentMethods)
    }

    @Test
    fun `on retrieve payment methods, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.failure(
                    cause = IllegalStateException("Failed to retrieve!"),
                    displayMessage = "Something went wrong!",
                ),
            ),
        )

        val result = dataSource.retrievePaymentMethods()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<List<PaymentMethod>>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on retrieve payment methods, should catch and fail if an exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to get payment methods!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onPaymentMethods = {
                    throw exception
                }
            )
        )

        val result = dataSource.retrievePaymentMethods()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<List<PaymentMethod>>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on retrieve payment option, should complete successfully from adapter`() = runTest {
        val paymentOptionId = "pm_1"
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(
                    value = CustomerAdapter.PaymentOption.fromId(paymentOptionId),
                ),
            ),
        )

        val result = dataSource.retrieveSavedSelection(null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<SavedSelection?>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(SavedSelection.PaymentMethod(paymentOptionId))
    }

    @Test
    fun `on retrieve payment option, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.failure(
                    cause = IllegalStateException("Failed to retrieve!"),
                    displayMessage = "Something went wrong!",
                )
            )
        )

        val result = dataSource.retrieveSavedSelection(null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<SavedSelection?>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on retrieve payment option, should catch and fail if an exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to retrieve saved selection!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onGetPaymentOption = {
                    throw exception
                }
            )
        )

        val result = dataSource.retrieveSavedSelection(null)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<SavedSelection?>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on set saved selection, should complete successfully from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onSetSelectedPaymentOption = {
                    CustomerAdapter.Result.success(Unit)
                }
            ),
        )

        val result = dataSource.setSavedSelection(SavedSelection.GooglePay)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<Unit>>()
    }

    @Test
    fun `on set saved selection, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onSetSelectedPaymentOption = {
                    CustomerAdapter.Result.failure(
                        cause = IllegalStateException("Failed to retrieve!"),
                        displayMessage = "Something went wrong!",
                    )
                }
            )
        )

        val result = dataSource.setSavedSelection(SavedSelection.GooglePay)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on set saved selection, should catch and fail if an exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to set selection!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onSetSelectedPaymentOption = {
                    throw exception
                }
            )
        )

        val result = dataSource.setSavedSelection(SavedSelection.GooglePay)

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<Unit>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on attach payment method, should complete successfully from adapter`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(id = "pm_1")
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.success(paymentMethod)
                }
            ),
        )

        val result = dataSource.attachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<PaymentMethod>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(paymentMethod)
    }

    @Test
    fun `on attach payment method, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onAttachPaymentMethod = {
                    CustomerAdapter.Result.failure(
                        cause = IllegalStateException("Failed to retrieve!"),
                        displayMessage = "Something went wrong!",
                    )
                }
            )
        )

        val result = dataSource.attachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on attach payment method, should catch and fail if an exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to attach!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onAttachPaymentMethod = {
                    throw exception
                }
            )
        )

        val result = dataSource.attachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on detach payment method, should complete successfully from adapter`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(id = "pm_1")
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.success(paymentMethod)
                },
            ),
        )

        val result = dataSource.detachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<PaymentMethod>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(paymentMethod)
    }

    @Test
    fun `on detach payment method, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    CustomerAdapter.Result.failure(
                        cause = IllegalStateException("Failed to retrieve!"),
                        displayMessage = "Something went wrong!",
                    )
                }
            )
        )

        val result = dataSource.detachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on detach payment method, should catch and fail if an exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to detach!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onDetachPaymentMethod = {
                    throw exception
                }
            )
        )

        val result = dataSource.detachPaymentMethod(paymentMethodId = "pm_1")

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on update payment method, should complete successfully from adapter`() = runTest {
        val paymentMethod = PaymentMethodFactory.card(id = "pm_1")
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onUpdatePaymentMethod = { _, _ ->
                    CustomerAdapter.Result.success(paymentMethod)
                },
            ),
        )

        val result = dataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(expiryYear = 2028, expiryMonth = 7),
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<PaymentMethod>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo(paymentMethod)
    }

    @Test
    fun `on update payment method, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onUpdatePaymentMethod = { _, _ ->
                    CustomerAdapter.Result.failure(
                        cause = IllegalStateException("Failed to retrieve!"),
                        displayMessage = "Something went wrong!",
                    )
                }
            )
        )

        val result = dataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(expiryYear = 2028, expiryMonth = 7),
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on update payment method, should catch and fail if an exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to update!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onUpdatePaymentMethod = { _, _ ->
                    throw exception
                }
            )
        )

        val result = dataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(expiryYear = 2028, expiryMonth = 7),
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<PaymentMethod>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on can create setup intents, should return true from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                canCreateSetupIntents = true,
            )
        )

        assertThat(dataSource.canCreateSetupIntents).isTrue()
    }

    @Test
    fun `on can create setup intents, should return false from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                canCreateSetupIntents = false,
            )
        )

        assertThat(dataSource.canCreateSetupIntents).isFalse()
    }

    @Test
    fun `on fetch setup intent client secret, should complete successfully from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.success("seti_example")
                }
            )
        )

        val result = dataSource.retrieveSetupIntentClientSecret()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<String>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo("seti_example")
    }

    @Test
    fun `on fetch setup intent client secret, should fail from adapter`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    CustomerAdapter.Result.failure(
                        cause = IllegalStateException("Failed to retrieve!"),
                        displayMessage = "Something went wrong!",
                    )
                }
            )
        )

        val result = dataSource.retrieveSetupIntentClientSecret()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<String>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on fetch setup intent client secret, should catch & fail if exception is thrown from adapter`() = runTest {
        val exception = IllegalStateException("Failed to update!")

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                onSetupIntentClientSecretForCustomerAttach = {
                    throw exception
                }
            )
        )

        val result = dataSource.retrieveSetupIntentClientSecret()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<String>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on load customer sheet session, should load properly & report success events`() = runTest {
        val intent = SetupIntentFactory.create()
        val elementsSessionRepository = createElementsSessionRepository(intent)

        val errorReporter = FakeErrorReporter()
        val paymentMethods = PaymentMethodFactory.cards(size = 4)
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(paymentMethods),
                selectedPaymentOption = CustomerAdapter.Result.success(
                    CustomerAdapter.PaymentOption.fromId(id = "pm_1")
                ),
            ),
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = errorReporter,
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<CustomerSheetSession>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.elementsSession.stripeIntent).isEqualTo(intent)
        assertThat(customerSheetSession.paymentMethods).containsExactlyElementsIn(paymentMethods)
        assertThat(customerSheetSession.savedSelection).isEqualTo(SavedSelection.PaymentMethod(id = "pm_1"))
        assertThat(customerSheetSession.permissions.canRemovePaymentMethods).isTrue()
        assertThat(customerSheetSession.paymentMethodSaveConsentBehavior).isEqualTo(
            PaymentMethodSaveConsentBehavior.Legacy
        )

        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_SUCCESS.eventName,
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_SUCCESS.eventName,
        )
    }

    @Test
    fun `on load with failed elements session load result, should fail to load & report properly`() = runTest {
        val errorReporter = FakeErrorReporter()

        val elementsSessionRepository = createElementsSessionRepository(
            error = IllegalStateException("Failed to load!")
        )
        val dataSource = createCustomerAdapterDataSource(
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = errorReporter,
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<CustomerSheetSession>>()

        val failureResult = result.asFailure()

        assertThat(failureResult.cause).isInstanceOf<IllegalStateException>()
        assertThat(failureResult.cause.message).isEqualTo("Failed to load!")
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_FAILURE.eventName,
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_SUCCESS.eventName,
        )
    }

    @Test
    fun `on load with failed PMs load result, should fail to load & report properly`() = runTest {
        val errorReporter = FakeErrorReporter()

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.failure(
                    cause = IllegalStateException("Failed to load!"),
                    displayMessage = null,
                ),
            ),
            errorReporter = errorReporter,
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<CustomerSheetSession>>()

        val failureResult = result.asFailure()

        assertThat(failureResult.cause).isInstanceOf<IllegalStateException>()
        assertThat(failureResult.cause.message).isEqualTo("Failed to load!")
        assertThat(errorReporter.getLoggedErrors()).containsExactly(
            ErrorReporter.SuccessEvent.CUSTOMER_SHEET_ELEMENTS_SESSION_LOAD_SUCCESS.eventName,
            ErrorReporter.ExpectedErrorEvent.CUSTOMER_SHEET_PAYMENT_METHODS_LOAD_FAILURE.eventName
        )
    }

    @Test
    fun `on load with failed saved selection result, should fail to load`() = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.failure(
                    cause = IllegalStateException("Failed to load!"),
                    displayMessage = null,
                )
            ),
        )

        val result = dataSource.loadCustomerSheetSession(createConfiguration())

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<CustomerSheetSession>>()

        val failureResult = result.asFailure()

        assertThat(failureResult.cause).isInstanceOf<IllegalStateException>()
        assertThat(failureResult.cause.message).isEqualTo("Failed to load!")
    }

    @Test
    fun `when SI can be created, elements session repo should be called with paymentMethodTypes`() =
        runPaymentMethodTypesTest(
            canCreateSetupIntents = true,
            providedPaymentMethodTypes = listOf("card", "us_bank_account", "sepa_debit"),
            expectedPaymentMethodTypes = listOf("card", "us_bank_account", "sepa_debit"),
        )

    @Test
    fun `when SI can be created and no paymentMethods, elements session repo should be called empty types list`() =
        runPaymentMethodTypesTest(
            canCreateSetupIntents = true,
            providedPaymentMethodTypes = null,
            expectedPaymentMethodTypes = listOf(),
        )

    @Test
    fun `when SI cannot be created, elements session repo should be called with card PM only`() =
        runPaymentMethodTypesTest(
            canCreateSetupIntents = false,
            providedPaymentMethodTypes = null,
            expectedPaymentMethodTypes = listOf("card")
        )

    @Test
    fun `when SI cannot be created, elements sessions is called with card only when paymentMethodTypes is set`() =
        runPaymentMethodTypesTest(
            canCreateSetupIntents = false,
            providedPaymentMethodTypes = listOf("card", "us_bank_account"),
            expectedPaymentMethodTypes = listOf("card")
        )

    @Test
    fun `when 'allowsRemovalOfLastSavedPaymentMethod' is true, 'canRemoveLastPaymentMethod should be true`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = true,
        )

    @Test
    fun `when 'allowsRemovalOfLastSavedPaymentMethod' is false, 'canRemoveLastPaymentMethod should be false`() =
        runRemoveLastPaymentMethodPermissionsTest(
            allowsRemovalOfLastSavedPaymentMethod = false,
        )

    private fun runRemoveLastPaymentMethodPermissionsTest(
        allowsRemovalOfLastSavedPaymentMethod: Boolean,
    ) = runTest {
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(),
            elementsSessionRepository = createElementsSessionRepository()
        )

        val result = dataSource.loadCustomerSheetSession(
            configuration = createConfiguration(
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
            )
        )

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<*>>()

        val customerSheetSession = result.asSuccess().value

        assertThat(customerSheetSession.permissions.canRemoveLastPaymentMethod)
            .isEqualTo(allowsRemovalOfLastSavedPaymentMethod)
    }

    private fun runPaymentMethodTypesTest(
        canCreateSetupIntents: Boolean,
        providedPaymentMethodTypes: List<String>?,
        expectedPaymentMethodTypes: List<String>,
    ) = runTest {
        val elementsSessionRepository = createElementsSessionRepository()

        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                canCreateSetupIntents = canCreateSetupIntents,
                paymentMethodTypes = providedPaymentMethodTypes
            ),
            elementsSessionRepository = elementsSessionRepository
        )

        dataSource.loadCustomerSheetSession(createConfiguration())

        val initializationMode = elementsSessionRepository.lastParams?.initializationMode

        assertThat(initializationMode).isInstanceOf<InitializationMode.DeferredIntent>()

        val deferredInitializationMode = initializationMode.asDeferred()

        assertThat(deferredInitializationMode.intentConfiguration.paymentMethodTypes)
            .containsExactlyElementsIn(expectedPaymentMethodTypes)
    }

    private suspend fun createCustomerAdapterDataSource(
        adapter: CustomerAdapter = FakeCustomerAdapter(),
        errorReporter: ErrorReporter = FakeErrorReporter(),
        elementsSessionRepository: ElementsSessionRepository = createElementsSessionRepository(),
    ): CustomerAdapterDataSource {
        return CustomerAdapterDataSource(
            customerAdapter = adapter,
            elementsSessionRepository = elementsSessionRepository,
            errorReporter = errorReporter,
            workContext = coroutineContext,
        )
    }

    private fun createElementsSessionRepository(
        intent: StripeIntent = SetupIntentFactory.create(),
        error: Throwable? = null,
    ): FakeElementsSessionRepository {
        return FakeElementsSessionRepository(
            error = error,
            linkSettings = null,
            stripeIntent = intent,
        )
    }

    @OptIn(ExperimentalAllowsRemovalOfLastSavedPaymentMethodApi::class)
    private fun createConfiguration(
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
    ): CustomerSheet.Configuration {
        return CustomerSheetFixtures.CONFIG_WITH_EVERYTHING.newBuilder()
            .allowsRemovalOfLastSavedPaymentMethod(allowsRemovalOfLastSavedPaymentMethod)
            .build()
    }

    private fun InitializationMode?.asDeferred(): InitializationMode.DeferredIntent {
        return this as InitializationMode.DeferredIntent
    }

    private fun <T> CustomerSheetDataResult<T>.asSuccess(): CustomerSheetDataResult.Success<T> {
        return this as CustomerSheetDataResult.Success<T>
    }

    private fun <T> CustomerSheetDataResult<T>.asFailure(): CustomerSheetDataResult.Failure<T> {
        return this as CustomerSheetDataResult.Failure<T>
    }
}
