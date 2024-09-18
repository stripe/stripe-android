package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.model.PaymentMethodUpdateParams
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.coroutineContext
import kotlin.test.Test

@OptIn(ExperimentalCustomerSheetApi::class)
class CustomerAdapterDataSourceTest {
    @Test
    fun `on retrieve payment methods, should completely successfully from adapter`() = runTest {
        val paymentMethods = PaymentMethodFactory.cards(size = 6)
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                paymentMethods = CustomerAdapter.Result.success(paymentMethods),
            ),
        )

        val successResult = dataSource.retrievePaymentMethods().asSuccess()

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

        val failedResult = dataSource.retrievePaymentMethods().asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    @Test
    fun `on retrieve payment option, should completely successfully from adapter`() = runTest {
        val paymentOptionId = "pm_1"
        val dataSource = createCustomerAdapterDataSource(
            adapter = FakeCustomerAdapter(
                selectedPaymentOption = CustomerAdapter.Result.success(
                    value = CustomerAdapter.PaymentOption.fromId(paymentOptionId),
                ),
            ),
        )

        val successResult = dataSource.retrieveSavedSelection().asSuccess()

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

        val failedResult = dataSource.retrieveSavedSelection().asFailure()

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

        val successResult = dataSource.attachPaymentMethod(paymentMethodId = "pm_1").asSuccess()

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

        val failedResult = dataSource.attachPaymentMethod(paymentMethodId = "pm_1").asFailure()

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

        val successResult = dataSource.detachPaymentMethod(paymentMethodId = "pm_1").asSuccess()

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

        val failedResult = dataSource.detachPaymentMethod(paymentMethodId = "pm_1").asFailure()

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

        val successResult = dataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(expiryYear = 2028, expiryMonth = 7),
        ).asSuccess()

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

        val failedResult = dataSource.updatePaymentMethod(
            paymentMethodId = "pm_1",
            params = PaymentMethodUpdateParams.createCard(expiryYear = 2028, expiryMonth = 7),
        ).asFailure()

        assertThat(failedResult.cause).isInstanceOf(IllegalStateException::class.java)
        assertThat(failedResult.cause.message).isEqualTo("Failed to retrieve!")
        assertThat(failedResult.displayMessage).isEqualTo("Something went wrong!")
    }

    private suspend fun createCustomerAdapterDataSource(
        adapter: CustomerAdapter = FakeCustomerAdapter(),
    ): CustomerAdapterDataSource {
        return CustomerAdapterDataSource(
            customerAdapter = adapter,
            workContext = coroutineContext,
        )
    }

    private fun <T> CustomerSheetDataResult<T>.asSuccess(): CustomerSheetDataResult.Success<T> {
        return this as CustomerSheetDataResult.Success<T>
    }

    private fun <T> CustomerSheetDataResult<T>.asFailure(): CustomerSheetDataResult.Failure<T> {
        return this as CustomerSheetDataResult.Failure<T>
    }
}
