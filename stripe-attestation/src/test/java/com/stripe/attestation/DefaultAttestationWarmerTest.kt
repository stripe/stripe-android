package com.stripe.attestation

import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.RetryDelaySupplier
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.time.Duration

class DefaultAttestationWarmerTest {

    @Test
    fun `start returns success without retrying`() = runScenario {
        val result = attestationWarmer.start()

        assertThat(result.isSuccess).isTrue()
        integrityRequestManager.prepareCalls.awaitItem()
    }

    @Test
    fun `start returns non-retriable failure without retrying`() {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.API_NOT_AVAILABLE,
            message = "API not available"
        )

        runScenario(
            prepareResults = listOf(Result.failure(error))
        ) {
            val result = attestationWarmer.start()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
            integrityRequestManager.prepareCalls.awaitItem()
        }
    }

    @Test
    fun `start returns unknown failure without retrying`() {
        val error = IllegalStateException("Unknown failure")

        runScenario(
            prepareResults = listOf(Result.failure(error))
        ) {
            val result = attestationWarmer.start()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
            integrityRequestManager.prepareCalls.awaitItem()
        }
    }

    @Test
    fun `start retries retriable failure until success`() {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.NETWORK_ERROR,
            message = "Network error"
        )

        runScenario(
            prepareResults = listOf(
                Result.failure(error),
                Result.success(Unit)
            )
        ) {
            val result = attestationWarmer.start()

            assertThat(result.isSuccess).isTrue()
            integrityRequestManager.prepareCalls.awaitItem()
            integrityRequestManager.prepareCalls.awaitItem()
            val delayCall = retryDelaySupplier.getDelayCalls.awaitItem()
            assertThat(delayCall.maxRetries).isEqualTo(3)
            assertThat(delayCall.remainingRetries).isEqualTo(3)
        }
    }

    @Test
    fun `start returns final failure after exhausting retries`() {
        val error = AttestationError(
            errorType = AttestationError.ErrorType.NETWORK_ERROR,
            message = "Network error"
        )

        runScenario(
            prepareResults = List(4) { Result.failure(error) }
        ) {
            val result = attestationWarmer.start()

            assertThat(result.exceptionOrNull()).isSameInstanceAs(error)
            repeat(4) {
                integrityRequestManager.prepareCalls.awaitItem()
            }
            val firstDelayCall = retryDelaySupplier.getDelayCalls.awaitItem()
            assertThat(firstDelayCall.maxRetries).isEqualTo(3)
            assertThat(firstDelayCall.remainingRetries).isEqualTo(3)

            val secondDelayCall = retryDelaySupplier.getDelayCalls.awaitItem()
            assertThat(secondDelayCall.maxRetries).isEqualTo(3)
            assertThat(secondDelayCall.remainingRetries).isEqualTo(2)

            val thirdDelayCall = retryDelaySupplier.getDelayCalls.awaitItem()
            assertThat(thirdDelayCall.maxRetries).isEqualTo(3)
            assertThat(thirdDelayCall.remainingRetries).isEqualTo(1)
        }
    }

    private fun runScenario(
        prepareResults: List<Result<Unit>> = listOf(Result.success(Unit)),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val integrityRequestManager = FakeIntegrityRequestManager(prepareResults)
        val retryDelaySupplier = FakeRetryDelaySupplier()
        val attestationWarmer = DefaultAttestationWarmer(
            integrityRequestManager = integrityRequestManager,
            retryDelaySupplier = retryDelaySupplier
        )
        val scenario = Scenario(
            attestationWarmer = attestationWarmer,
            integrityRequestManager = integrityRequestManager,
            retryDelaySupplier = retryDelaySupplier
        )

        scenario.block()

        integrityRequestManager.ensureAllEventsConsumed()
        retryDelaySupplier.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val attestationWarmer: DefaultAttestationWarmer,
        val integrityRequestManager: FakeIntegrityRequestManager,
        val retryDelaySupplier: FakeRetryDelaySupplier
    )
}

internal class FakeIntegrityRequestManager(
    prepareResults: List<Result<Unit>>
) : IntegrityRequestManager {
    val prepareCalls = Turbine<Unit>()
    val requestTokenCalls = Turbine<String?>()
    private val prepareResults = ArrayDeque(prepareResults)

    override suspend fun prepare(): Result<Unit> {
        prepareCalls.add(Unit)
        return prepareResults.removeFirst()
    }

    override suspend fun requestToken(requestIdentifier: String?): Result<String> {
        requestTokenCalls.add(requestIdentifier)
        error("Unexpected requestToken call")
    }

    fun ensureAllEventsConsumed() {
        prepareCalls.ensureAllEventsConsumed()
        requestTokenCalls.ensureAllEventsConsumed()
    }
}

internal class FakeRetryDelaySupplier : RetryDelaySupplier {
    val getDelayCalls = Turbine<GetDelayCall>()
    val maxDurationCalls = Turbine<Int>()

    override fun maxDuration(maxRetries: Int): Duration {
        maxDurationCalls.add(maxRetries)
        error("Unexpected maxDuration call")
    }

    override fun getDelay(maxRetries: Int, remainingRetries: Int): Duration {
        getDelayCalls.add(GetDelayCall(maxRetries, remainingRetries))
        return Duration.ZERO
    }

    fun ensureAllEventsConsumed() {
        getDelayCalls.ensureAllEventsConsumed()
        maxDurationCalls.ensureAllEventsConsumed()
    }

    data class GetDelayCall(
        val maxRetries: Int,
        val remainingRetries: Int
    )
}
