package com.stripe.android.common.taptoadd

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.RetryDelaySupplier
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration

internal class TapToAddRetriableConnectionManagerTest {
    @Test
    fun `isSupported is true when inner manager's isSupported is true`() = runScenario(isSupported = true) {
        assertThat(retryConnectionManager.isSupported).isTrue()
    }

    @Test
    fun `isSupported is false when inner manager's isSupported is false`() = runScenario(isSupported = false) {
        assertThat(retryConnectionManager.isSupported).isFalse()
    }

    @Test
    fun `connect succeeds on first try and calls delegate once`() = runScenario(
        queuedConnectResults = listOf(Result.success(Unit))
    ) {
        retryConnectionManager.connect()

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `connect retries on failure then succeeds`() = runScenario(
        queuedConnectResults = listOf(
            Result.failure(IllegalStateException("Failed!")),
            Result.success(Unit)
        )
    ) {
        retryConnectionManager.connect()

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()

        val delayCall = getRetryDelayCalls.awaitItem()

        assertThat(delayCall.maxRetries).isEqualTo(3)
        assertThat(delayCall.remainingRetries).isEqualTo(3)

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()
    }

    @Test
    fun `connect throws after all retries exhausted`() = runScenario(
        queuedConnectResults = listOf(
            Result.failure(IllegalStateException("Failed!")),
            Result.failure(IllegalStateException("Failed!")),
            Result.failure(IllegalStateException("Failed!")),
            Result.failure(IllegalStateException("Failed!")),
        )
    ) {
        val error = assertFailsWith<IllegalStateException> {
            retryConnectionManager.connect()
        }

        assertThat(error.message).isEqualTo("Failed!")

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()

        val firstDelayCall = getRetryDelayCalls.awaitItem()

        assertThat(firstDelayCall.maxRetries).isEqualTo(3)
        assertThat(firstDelayCall.remainingRetries).isEqualTo(3)

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()

        val secondDelayCall = getRetryDelayCalls.awaitItem()

        assertThat(secondDelayCall.maxRetries).isEqualTo(3)
        assertThat(secondDelayCall.remainingRetries).isEqualTo(2)

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()

        val thirdDelayCall = getRetryDelayCalls.awaitItem()

        assertThat(thirdDelayCall.maxRetries).isEqualTo(3)
        assertThat(thirdDelayCall.remainingRetries).isEqualTo(1)

        assertThat(innerManagerConnectCalls.awaitItem()).isNotNull()
    }

    private fun runScenario(
        isSupported: Boolean = true,
        queuedConnectResults: List<Result<Unit>> = listOf(Result.success(Unit)),
        block: suspend Scenario.() -> Unit
    ) = runTest {
        FakeTapToAddConnectionManager.test(
            isSupported = isSupported,
            connectResults = queuedConnectResults,
        ) {
            val retryDelaySupplier = FakeRetryDelaySupplier()

            block(
                Scenario(
                    retryConnectionManager = TapToAddRetriableConnectionManager(
                        tapToAddConnectionManager = tapToAddConnectionManager,
                        retryDelaySupplier = retryDelaySupplier,
                    ),
                    innerManagerConnectCalls = connectCalls,
                    getRetryDelayCalls = retryDelaySupplier.getDelayCalls,
                )
            )

            retryDelaySupplier.validate()
        }
    }

    private class Scenario(
        val retryConnectionManager: TapToAddConnectionManager,
        val innerManagerConnectCalls: ReceiveTurbine<Unit>,
        val getRetryDelayCalls: ReceiveTurbine<FakeRetryDelaySupplier.GetDelayCall>,
    )

    private class FakeRetryDelaySupplier : RetryDelaySupplier {
        val getDelayCalls = Turbine<GetDelayCall>()

        override fun maxDuration(maxRetries: Int): Duration {
            throw IllegalStateException("Should not be called!")
        }

        override fun getDelay(maxRetries: Int, remainingRetries: Int): Duration {
            getDelayCalls.add(GetDelayCall(maxRetries, remainingRetries))

            return Duration.ZERO
        }

        fun validate() {
            getDelayCalls.ensureAllEventsConsumed()
        }

        data class GetDelayCall(
            val maxRetries: Int,
            val remainingRetries: Int,
        )
    }
}
