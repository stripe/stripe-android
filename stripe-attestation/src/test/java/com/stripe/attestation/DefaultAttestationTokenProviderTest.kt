package com.stripe.attestation

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultAttestationTokenProviderTest {

    @Test
    fun `getToken returns successful token without resetting`() = runScenario(
        requestTokenResults = listOf(Result.success("token"))
    ) {
        val result = attestationTokenProvider.getToken()

        assertThat(result.getOrNull()).isEqualTo("token")
        assertThat(integrityRequestManager.requestTokenCalls.awaitItem()).isNull()
    }

    @Test
    fun `getToken returns non-provider-invalid failure without resetting`() {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.NETWORK_ERROR,
            message = "Network error"
        )

        runScenario(
            requestTokenResults = listOf(Result.failure(error))
        ) {
            val result = attestationTokenProvider.getToken()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
            assertThat(integrityRequestManager.requestTokenCalls.awaitItem()).isNull()
        }
    }

    @Test
    fun `getToken resets provider and retries provider-invalid failure once`() {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID,
            message = "Integrity token provider is invalid"
        )

        runScenario(
            requestTokenResults = listOf(
                Result.failure(error),
                Result.success("refreshed-token")
            )
        ) {
            val result = attestationTokenProvider.getToken()

            assertThat(result.getOrNull()).isEqualTo("refreshed-token")
            assertThat(integrityRequestManager.requestTokenCalls.awaitItem()).isNull()
            integrityRequestManager.resetCalls.awaitItem()
            assertThat(integrityRequestManager.requestTokenCalls.awaitItem()).isNull()
        }
    }

    @Test
    fun `getToken returns second provider-invalid failure without retrying again`() {
        val firstError = AttestationError(
            errorType = AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID,
            message = "First provider is invalid"
        )
        val secondError = AttestationError(
            errorType = AttestationError.ErrorType.INTEGRITY_TOKEN_PROVIDER_INVALID,
            message = "Second provider is invalid"
        )

        runScenario(
            requestTokenResults = listOf(
                Result.failure(firstError),
                Result.failure(secondError)
            )
        ) {
            val result = attestationTokenProvider.getToken()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(secondError)
            assertThat(integrityRequestManager.requestTokenCalls.awaitItem()).isNull()
            integrityRequestManager.resetCalls.awaitItem()
            assertThat(integrityRequestManager.requestTokenCalls.awaitItem()).isNull()
        }
    }

    private fun runScenario(
        requestTokenResults: List<Result<String>>,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val integrityRequestManager = FakeTokenIntegrityRequestManager(requestTokenResults)
        val attestationTokenProvider = DefaultAttestationTokenProvider(integrityRequestManager)

        Scenario(
            attestationTokenProvider = attestationTokenProvider,
            integrityRequestManager = integrityRequestManager
        ).apply { block() }

        integrityRequestManager.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val attestationTokenProvider: DefaultAttestationTokenProvider,
        val integrityRequestManager: FakeTokenIntegrityRequestManager
    )
}

internal class FakeTokenIntegrityRequestManager(
    requestTokenResults: List<Result<String>>
) : IntegrityRequestManager {
    val prepareCalls = Turbine<Unit>()
    val requestTokenCalls = Turbine<String?>()
    val resetCalls = Turbine<Unit>()
    private val requestTokenResults = ArrayDeque(requestTokenResults)

    override suspend fun prepare(): Result<Unit> {
        prepareCalls.add(Unit)
        error("Unexpected prepare call")
    }

    override suspend fun requestToken(requestIdentifier: String?): Result<String> {
        requestTokenCalls.add(requestIdentifier)
        return requestTokenResults.removeFirst()
    }

    override suspend fun reset() {
        resetCalls.add(Unit)
    }

    fun ensureAllEventsConsumed() {
        prepareCalls.ensureAllEventsConsumed()
        requestTokenCalls.ensureAllEventsConsumed()
        resetCalls.ensureAllEventsConsumed()
    }
}
