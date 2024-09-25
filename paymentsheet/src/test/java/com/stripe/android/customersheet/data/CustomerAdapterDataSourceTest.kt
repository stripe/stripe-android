package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.isInstanceOf
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

@OptIn(ExperimentalCustomerSheetApi::class)
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
    fun `on retrieve payment option, should complete successfully from adapter`() = runTest {
        val paymentOptionId = "pm_1"
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(
                    value = CustomerAdapter.PaymentOption.fromId(paymentOptionId),
                ),
            ),
        )

        val result = dataSource.retrieveSavedSelection()

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

        val result = dataSource.retrieveSavedSelection()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<SavedSelection?>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
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

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<SavedSelection?>>()
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

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<SavedSelection?>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
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

    private fun createCustomerAdapterDataSource(
        adapter: CustomerAdapter = FakeCustomerAdapter(),
    ): CustomerAdapterDataSource {
        return CustomerAdapterDataSource(
            customerAdapter = adapter,
        )
    }

    private fun <T> CustomerSheetDataResult<T>.asSuccess(): CustomerSheetDataResult.Success<T> {
        return this as CustomerSheetDataResult.Success<T>
    }

    private fun <T> CustomerSheetDataResult<T>.asFailure(): CustomerSheetDataResult.Failure<T> {
        return this as CustomerSheetDataResult.Failure<T>
    }
}
