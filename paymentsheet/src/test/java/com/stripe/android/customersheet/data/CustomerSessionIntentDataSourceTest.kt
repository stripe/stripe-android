package com.stripe.android.customersheet.data

import com.google.common.truth.Truth.assertThat
import com.stripe.android.customersheet.CustomerSheet
import com.stripe.android.customersheet.utils.FakeCustomerSessionProvider
import com.stripe.android.isInstanceOf
import com.stripe.android.paymentsheet.ExperimentalCustomerSessionApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCustomerSessionApi::class)
class CustomerSessionIntentDataSourceTest {
    @Test
    fun `on can create setup intents, should return true`() {
        val dataSource = createIntentDataSource()

        assertThat(dataSource.canCreateSetupIntents).isTrue()
    }

    @Test
    fun `on provide setup intent secret, should return secret from customer session provider`() = runTest {
        val dataSource = createIntentDataSource(
            customerSessionProvider = FakeCustomerSessionProvider(
                onProvideSetupIntentClientSecret = {
                    Result.success("seti_123_secret_123")
                }
            ),
        )

        val result = dataSource.retrieveSetupIntentClientSecret()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Success<String>>()

        val successResult = result.asSuccess()

        assertThat(successResult.value).isEqualTo("seti_123_secret_123")
    }

    @Test
    fun `on provide setup intent client secret, should fail if secret retrieval from provider fails`() = runTest {
        val exception = IllegalStateException("Failed to provide secret!")
        val dataSource = createIntentDataSource(
            customerSessionProvider = FakeCustomerSessionProvider(
                onProvideSetupIntentClientSecret = {
                    Result.failure(exception)
                }
            ),
        )

        val result = dataSource.retrieveSetupIntentClientSecret()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<String>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    @Test
    fun `on provide setup intent client secret, should fail if ephemeral key fetch fails`() = runTest {
        val exception = IllegalStateException("Failed to provide ephemeral key!")
        val dataSource = createIntentDataSource(
            elementsSessionManager = FakeCustomerSessionElementsSessionManager(
                ephemeralKey = Result.failure(exception)
            )
        )

        val result = dataSource.retrieveSetupIntentClientSecret()

        assertThat(result).isInstanceOf<CustomerSheetDataResult.Failure<String>>()

        val failedResult = result.asFailure()

        assertThat(failedResult.cause).isEqualTo(exception)
    }

    private fun createIntentDataSource(
        elementsSessionManager: CustomerSessionElementsSessionManager = FakeCustomerSessionElementsSessionManager(),
        customerSessionProvider: CustomerSheet.CustomerSessionProvider = FakeCustomerSessionProvider()
    ): CustomerSheetIntentDataSource {
        return CustomerSessionIntentDataSource(
            elementsSessionManager = elementsSessionManager,
            customerSessionProvider = customerSessionProvider,
        )
    }

    private fun <T> CustomerSheetDataResult<T>.asSuccess(): CustomerSheetDataResult.Success<T> {
        return this as CustomerSheetDataResult.Success<T>
    }

    private fun <T> CustomerSheetDataResult<T>.asFailure(): CustomerSheetDataResult.Failure<T> {
        return this as CustomerSheetDataResult.Failure<T>
    }
}
