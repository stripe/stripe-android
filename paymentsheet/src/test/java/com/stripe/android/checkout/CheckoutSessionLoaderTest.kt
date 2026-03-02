package com.stripe.android.checkout

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionRepository
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.ConfirmCheckoutSessionParams
import kotlinx.coroutines.test.runTest
import org.junit.Test

class CheckoutSessionLoaderTest {

    @Test
    fun `load extracts session ID and returns response on success`() = runTest {
        val expectedResponse = CheckoutSessionResponse(
            id = "cs_test_abc123",
            amount = 1000L,
            currency = "usd",
        )
        val repository = FakeCheckoutRepository(
            initCheckoutSessionResult = Result.success(expectedResponse),
        )
        val loader = CheckoutSessionLoader(
            repository = repository,
        )

        val result = loader.load("cs_test_abc123_secret_xyz")

        assertThat(result.isSuccess).isTrue()
        assertThat(result.getOrThrow()).isEqualTo(expectedResponse)
        assertThat(repository.lastInitSessionId).isEqualTo("cs_test_abc123")
    }

    @Test
    fun `load returns failure when repository fails`() = runTest {
        val error = RuntimeException("Network error")
        val repository = FakeCheckoutRepository(
            initCheckoutSessionResult = Result.failure(error),
        )
        val loader = CheckoutSessionLoader(
            repository = repository,
        )

        val result = loader.load("cs_test_abc123_secret_xyz")

        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()).isEqualTo(error)
    }

    private class FakeCheckoutRepository(
        private val initCheckoutSessionResult: Result<CheckoutSessionResponse>,
    ) : CheckoutSessionRepository {
        var lastInitSessionId: String? = null
            private set

        override suspend fun init(
            sessionId: String,
        ): Result<CheckoutSessionResponse> {
            lastInitSessionId = sessionId
            return initCheckoutSessionResult
        }

        override suspend fun confirm(
            id: String,
            params: ConfirmCheckoutSessionParams,
        ): Result<CheckoutSessionResponse> {
            throw NotImplementedError()
        }
    }
}
