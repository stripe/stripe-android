package com.stripe.attestation

import app.cash.turbine.Turbine
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.PrepareIntegrityTokenRequest
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.createStandardIntegrityException
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.RetryDelaySupplier
import com.stripe.android.testing.FakeLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Test
import kotlin.time.Duration

class DefaultIntegrityTokenProviderFactoryTest {

    @Test
    fun `warmup returns success and caches provider`() {
        val expectedProvider = FakePreparedIntegrityTokenProvider()

        runScenario(
            prepareTasks = listOf(Tasks.forResult(expectedProvider))
        ) {
            val warmupResult = provider.warmup()
            val providerResult = provider.integrityTokenProvider(allowRetry = false)

            assertThat(warmupResult.getOrNull()).isEqualTo(Unit)
            assertThat(providerResult.getOrNull()).isSameInstanceAs(expectedProvider)
            assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
            assertThat(factory.prepareCalls.awaitItem()).isNotNull()
            retryDelaySupplier.getDelayCalls.expectNoEvents()
            assertThat(logger.errorLogs).isEmpty()
        }
    }

    @Test
    fun `warmup preserves non-retryable error`() = runScenario(
        prepareTasks = listOf(failureTask(StandardIntegrityErrorCode.API_NOT_AVAILABLE))
    ) {
        val result = provider.warmup()

        assertThat(result.exceptionOrNull()).isInstanceOf(AttestationError::class.java)
        assertThat((result.exceptionOrNull() as AttestationError).errorType)
            .isEqualTo(AttestationError.ErrorType.API_NOT_AVAILABLE)
        assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
        assertThat(factory.prepareCalls.awaitItem()).isNotNull()
        assertThat(logger.errorLogs.single().first)
            .isEqualTo("Integrity - Failed to prepare integrity token")
        retryDelaySupplier.getDelayCalls.expectNoEvents()
    }

    @Test
    fun `returns provider on first attempt with cloud project number`() {
        val expectedProvider = FakePreparedIntegrityTokenProvider()

        runScenario(
            prepareTasks = listOf(Tasks.forResult(expectedProvider))
        ) {
            val result = provider.integrityTokenProvider(allowRetry = false)

            assertThat(result.getOrNull()).isSameInstanceAs(expectedProvider)
            assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
            assertThat(factory.prepareCalls.awaitItem().toString())
                .contains("cloudProjectNumber=$CLOUD_PROJECT_NUMBER")
            retryDelaySupplier.getDelayCalls.expectNoEvents()
            assertThat(logger.errorLogs).isEmpty()
        }
    }

    @Test
    fun `concurrent calls prepare only once`() {
        val expectedProvider = FakePreparedIntegrityTokenProvider()
        val prepareTask = TaskCompletionSource<StandardIntegrityTokenProvider>()

        runScenario(
            prepareTasks = listOf(prepareTask.task)
        ) {
            val results = coroutineScope {
                val requests = List(10) {
                    async { provider.integrityTokenProvider(allowRetry = false) }
                }
                yield()
                prepareTask.setResult(expectedProvider)
                requests.awaitAll()
            }

            results.forEach { result ->
                assertThat(result.getOrNull()).isSameInstanceAs(expectedProvider)
            }
            assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
            assertThat(factory.prepareCalls.awaitItem()).isNotNull()
            retryDelaySupplier.getDelayCalls.expectNoEvents()
            assertThat(logger.errorLogs).isEmpty()
        }
    }

    @Test
    fun `does not retry when retry is not allowed`() = runScenario(
        prepareTasks = listOf(failureTask(StandardIntegrityErrorCode.NETWORK_ERROR))
    ) {
        val result = provider.integrityTokenProvider(allowRetry = false)

        assertThat(result.exceptionOrNull()).isInstanceOf(AttestationError::class.java)
        assertThat((result.exceptionOrNull() as AttestationError).errorType)
            .isEqualTo(AttestationError.ErrorType.NETWORK_ERROR)
        assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
        assertThat(factory.prepareCalls.awaitItem()).isNotNull()
        assertThat(logger.errorLogs.single().first)
            .isEqualTo("Integrity - Failed to prepare integrity token")
        retryDelaySupplier.getDelayCalls.expectNoEvents()
    }

    @Test
    fun `retries retryable error when retry is allowed`() {
        val expectedProvider = FakePreparedIntegrityTokenProvider()

        runScenario(
            prepareTasks = listOf(
                failureTask(StandardIntegrityErrorCode.NETWORK_ERROR),
                Tasks.forResult(expectedProvider)
            )
        ) {
            val result = provider.integrityTokenProvider(allowRetry = true)

            assertThat(result.getOrNull()).isSameInstanceAs(expectedProvider)
            assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
            repeat(2) {
                assertThat(factory.prepareCalls.awaitItem()).isNotNull()
            }
            assertThat(logger.errorLogs.single().first)
                .isEqualTo("Integrity - Failed to prepare integrity token")
            assertThat(retryDelaySupplier.getDelayCalls.awaitItem()).isEqualTo(
                FakeAttestationRetryDelaySupplier.GetDelayCall(
                    maxRetries = 3,
                    remainingRetries = 3
                )
            )
        }
    }

    @Test
    fun `does not retry non-retryable error when retry is allowed`() = runScenario(
        prepareTasks = listOf(failureTask(StandardIntegrityErrorCode.API_NOT_AVAILABLE))
    ) {
        val result = provider.integrityTokenProvider(allowRetry = true)

        assertThat(result.exceptionOrNull()).isInstanceOf(AttestationError::class.java)
        assertThat((result.exceptionOrNull() as AttestationError).errorType)
            .isEqualTo(AttestationError.ErrorType.API_NOT_AVAILABLE)
        assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
        assertThat(factory.prepareCalls.awaitItem()).isNotNull()
        assertThat(logger.errorLogs.single().first)
            .isEqualTo("Integrity - Failed to prepare integrity token")
        retryDelaySupplier.getDelayCalls.expectNoEvents()
    }

    @Test
    fun `stops after three retries`() = runScenario(
        prepareTasks = List(4) {
            failureTask(StandardIntegrityErrorCode.NETWORK_ERROR)
        }
    ) {
        val result = provider.integrityTokenProvider(allowRetry = true)

        assertThat(result.exceptionOrNull()).isInstanceOf(AttestationError::class.java)
        assertThat((result.exceptionOrNull() as AttestationError).errorType)
            .isEqualTo(AttestationError.ErrorType.NETWORK_ERROR)
        assertThat(factory.createCalls.awaitItem()).isEqualTo(Unit)
        repeat(4) { attempt ->
            assertThat(factory.prepareCalls.awaitItem()).isNotNull()
            assertThat(logger.errorLogs[attempt].first)
                .isEqualTo("Integrity - Failed to prepare integrity token")
        }
        listOf(3, 2, 1).forEach { remainingRetries ->
            assertThat(retryDelaySupplier.getDelayCalls.awaitItem()).isEqualTo(
                FakeAttestationRetryDelaySupplier.GetDelayCall(
                    maxRetries = 3,
                    remainingRetries = remainingRetries
                )
            )
        }
    }

    private fun runScenario(
        prepareTasks: List<Task<StandardIntegrityTokenProvider>>,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val factory = FakeRetryStandardIntegrityManagerFactory(prepareTasks)
        val retryDelaySupplier = FakeAttestationRetryDelaySupplier()
        val logger = FakeLogger()
        val provider = DefaultIntegrityTokenProviderFactory(
            cloudProjectNumber = CLOUD_PROJECT_NUMBER,
            factory = factory,
            logger = logger,
            retryDelaySupplier = retryDelaySupplier,
            mutex = Mutex()
        )

        Scenario(
            provider = provider,
            factory = factory,
            retryDelaySupplier = retryDelaySupplier,
            logger = logger
        ).apply { block() }

        factory.ensureAllEventsConsumed()
        retryDelaySupplier.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val provider: DefaultIntegrityTokenProviderFactory,
        val factory: FakeRetryStandardIntegrityManagerFactory,
        val retryDelaySupplier: FakeAttestationRetryDelaySupplier,
        val logger: FakeLogger
    )

    private companion object {
        const val CLOUD_PROJECT_NUMBER = 123456789L

        fun failureTask(errorCode: Int): Task<StandardIntegrityTokenProvider> {
            return Tasks.forException(createStandardIntegrityException(errorCode))
        }
    }
}

internal class FakePreparedIntegrityTokenProvider : StandardIntegrityTokenProvider {
    override fun request(request: StandardIntegrityTokenRequest): Task<StandardIntegrityToken> {
        error("request should not be called")
    }
}

internal class FakeRetryStandardIntegrityManagerFactory(
    prepareTasks: List<Task<StandardIntegrityTokenProvider>>
) : StandardIntegrityManagerFactory {
    val createCalls = Turbine<Unit>()
    val prepareCalls = Turbine<PrepareIntegrityTokenRequest>()

    private val prepareTasks = ArrayDeque(prepareTasks)

    override fun create(): StandardIntegrityManager {
        createCalls.add(Unit)
        return StandardIntegrityManager { request ->
            prepareCalls.add(request)
            prepareTasks.removeFirst()
        }
    }

    fun ensureAllEventsConsumed() {
        createCalls.ensureAllEventsConsumed()
        prepareCalls.ensureAllEventsConsumed()
    }
}

internal class FakeAttestationRetryDelaySupplier : RetryDelaySupplier {
    val getDelayCalls = Turbine<GetDelayCall>()

    override fun maxDuration(maxRetries: Int): Duration {
        error("maxDuration should not be called")
    }

    override fun getDelay(maxRetries: Int, remainingRetries: Int): Duration {
        getDelayCalls.add(GetDelayCall(maxRetries, remainingRetries))
        return Duration.ZERO
    }

    fun ensureAllEventsConsumed() {
        getDelayCalls.ensureAllEventsConsumed()
    }

    data class GetDelayCall(
        val maxRetries: Int,
        val remainingRetries: Int
    )
}
