package com.stripe.attestation

import android.app.Activity
import app.cash.turbine.Turbine
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class IntegrityStandardRequestManagerTest {

    @Test
    fun `requestToken returns token`() = runScenario {
        val result = requestManager.requestToken("requestIdentifier")

        assertThat(result.getOrNull()).isEqualTo("123456789")
        assertThat(tokenProviderFactory.integrityTokenProviderCalls.awaitItem()).isFalse()
        assertThat(tokenProvider.requestCalls.awaitItem()).isNotNull()
        logErrorCalls.expectNoEvents()
    }

    @Test
    fun `requestToken maps Play Integrity failure`() {
        val exception = mock<StandardIntegrityException> {
            on { errorCode } doReturn StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND
        }

        runScenario(
            requestTask = Tasks.forException(exception)
        ) {
            val result = requestManager.requestToken("requestIdentifier")

            assertThat(result.exceptionOrNull()).isInstanceOf(AttestationError::class.java)
            assertThat((result.exceptionOrNull() as AttestationError).errorType)
                .isEqualTo(AttestationError.ErrorType.PLAY_SERVICES_NOT_FOUND)
            assertThat(tokenProviderFactory.integrityTokenProviderCalls.awaitItem()).isFalse()
            assertThat(tokenProvider.requestCalls.awaitItem()).isNotNull()
            assertThat(logErrorCalls.awaitItem().message)
                .isEqualTo("Integrity - Failed to request integrity token")
        }
    }

    @Test
    fun `requestToken preserves provider factory failure`() {
        val expectedError = AttestationError(
            errorType = AttestationError.ErrorType.NETWORK_ERROR,
            message = "Failed to create token provider"
        )

        runScenario(
            tokenProviderResult = Result.failure(expectedError)
        ) {
            val result = requestManager.requestToken("requestIdentifier")

            assertThat(result.exceptionOrNull()).isSameInstanceAs(expectedError)
            assertThat(tokenProviderFactory.integrityTokenProviderCalls.awaitItem()).isFalse()
            tokenProvider.requestCalls.expectNoEvents()
            assertThat(logErrorCalls.awaitItem().message)
                .isEqualTo("Integrity - Failed to request integrity token")
        }
    }

    private fun runScenario(
        requestTask: Task<StandardIntegrityToken> = Tasks.forResult(FakeStandardIntegrityToken()),
        tokenProviderResult: Result<StandardIntegrityTokenProvider>? = null,
        block: suspend Scenario.() -> Unit
    ) = runTest {
        val tokenProvider = FakeStandardIntegrityTokenProvider(requestTask)
        val tokenProviderFactory = FakeIntegrityTokenProviderFactory(
            tokenProviderResult = tokenProviderResult ?: Result.success(tokenProvider)
        )
        val logErrorCalls = Turbine<RequestLogErrorCall>()
        val requestManager = IntegrityStandardRequestManager(
            integrityTokenProviderFactory = tokenProviderFactory,
            logError = { message, error ->
                logErrorCalls.add(RequestLogErrorCall(message, error))
            }
        )

        Scenario(
            requestManager = requestManager,
            tokenProviderFactory = tokenProviderFactory,
            tokenProvider = tokenProvider,
            logErrorCalls = logErrorCalls
        ).apply { block() }

        tokenProviderFactory.ensureAllEventsConsumed()
        tokenProvider.ensureAllEventsConsumed()
        logErrorCalls.ensureAllEventsConsumed()
    }

    private data class Scenario(
        val requestManager: IntegrityRequestManager,
        val tokenProviderFactory: FakeIntegrityTokenProviderFactory,
        val tokenProvider: FakeStandardIntegrityTokenProvider,
        val logErrorCalls: Turbine<RequestLogErrorCall>
    )
}

internal class FakeIntegrityTokenProviderFactory(
    var tokenProviderResult: Result<StandardIntegrityTokenProvider>
) : IntegrityTokenProviderFactory {
    val integrityTokenProviderCalls = Turbine<Boolean>()

    override suspend fun integrityTokenProvider(
        allowRetry: Boolean
    ): Result<StandardIntegrityTokenProvider> {
        integrityTokenProviderCalls.add(allowRetry)
        return tokenProviderResult
    }

    fun ensureAllEventsConsumed() {
        integrityTokenProviderCalls.ensureAllEventsConsumed()
    }
}

internal class FakeStandardIntegrityTokenProvider(
    private val requestTask: Task<StandardIntegrityToken>
) : StandardIntegrityTokenProvider {
    val requestCalls = Turbine<StandardIntegrityTokenRequest>()

    override fun request(request: StandardIntegrityTokenRequest): Task<StandardIntegrityToken> {
        requestCalls.add(request)
        return requestTask
    }

    fun ensureAllEventsConsumed() {
        requestCalls.ensureAllEventsConsumed()
    }
}

internal class FakeStandardIntegrityToken : StandardIntegrityToken() {
    override fun showDialog(p0: Activity?, p1: Int) = Tasks.forResult(0)
    override fun token(): String = "123456789"
}

internal data class RequestLogErrorCall(
    val message: String,
    val error: Throwable
)
