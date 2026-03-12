package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.FakeCheckoutSessionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckoutSessionLoaderTest {

    @Test
    fun `load extracts session ID and returns response on success`() = runTest {
        val expectedResponse = CheckoutSessionResponse(
            id = "cs_test_abc123",
            amount = 1000L,
            currency = "usd",
            mode = CheckoutSessionResponse.Mode.PAYMENT,
            customerEmail = null,
            elementsSession = null,
            paymentIntent = null,
            setupIntent = null,
            customer = null,
            savedPaymentMethodsOfferSave = null,
            totalSummary = null,
            lineItems = emptyList(),
            shippingOptions = emptyList(),
        )
        val repository = FakeCheckoutSessionRepository(
            initResult = Result.success(expectedResponse),
        )
        val loader = CheckoutSessionLoader(
            repository = repository,
        )

        val result = loader.load("cs_test_abc123_secret_xyz")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(expectedResponse)
        assertThat(repository.initRequests.awaitItem()).isEqualTo("cs_test_abc123")
    }

    @Test
    fun `load returns failure when repository fails`() = runTest {
        val error = RuntimeException("Network error")
        val repository = FakeCheckoutSessionRepository(
            initResult = Result.failure(error),
        )
        val loader = CheckoutSessionLoader(
            repository = repository,
        )

        val result = loader.load("cs_test_abc123_secret_xyz")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }
}
