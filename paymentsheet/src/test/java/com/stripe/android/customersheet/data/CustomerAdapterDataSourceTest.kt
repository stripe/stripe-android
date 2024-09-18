package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerAdapter
import com.stripe.android.customersheet.ExperimentalCustomerSheetApi
import com.stripe.android.customersheet.FakeCustomerAdapter
import com.stripe.android.paymentsheet.model.SavedSelection
import com.stripe.android.testing.PaymentMethodFactory
import kotlinx.coroutines.test.runTest
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
