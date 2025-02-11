package com.stripe.attestation

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.StandardIntegrityException
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import com.google.android.play.core.integrity.model.StandardIntegrityErrorCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.Test
import kotlin.test.assertEquals

class IntegrityStandardRequestManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `prepare - success returns successful result`() = runTest {
        val tokenProvider = FakeStandardIntegrityTokenProvider(Tasks.forResult(FakeStandardIntegrityToken()))
        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forResult(tokenProvider),
        )

        val result = integrityStandardRequestManager.prepare()

        assert(result.isSuccess)
    }

    @Test
    fun `prepare - failure on prepare task returns Attestation error`() = runTest {
        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forException(Exception("Failed to build token provider")),
        )

        val result = integrityStandardRequestManager.prepare()

        assert(result.isFailure)
        assert(result.exceptionOrNull() is AttestationError)
    }

    @Test
    fun `requestToken - success`() = runTest {
        val tokenProvider = FakeStandardIntegrityTokenProvider(Tasks.forResult(FakeStandardIntegrityToken()))
        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forResult(tokenProvider),
        )

        integrityStandardRequestManager.prepare()
        val result = integrityStandardRequestManager.requestToken("requestIdentifier")

        assert(result.isSuccess)
    }

    @Test
    fun `requestToken - failure returns Attestation error with correct type`() = runTest {
        val exception = mock<StandardIntegrityException> {
            on { errorCode } doReturn StandardIntegrityErrorCode.PLAY_SERVICES_NOT_FOUND
        }
        val tokenProvider = FakeStandardIntegrityTokenProvider(Tasks.forException(exception))

        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forResult(tokenProvider),
        )

        integrityStandardRequestManager.prepare()
        val result = integrityStandardRequestManager.requestToken("requestIdentifier")

        assert(result.isFailure)
        assert(result.exceptionOrNull() is AttestationError)
        val error = result.exceptionOrNull() as AttestationError
        assertEquals(error.errorType, AttestationError.ErrorType.PLAY_SERVICES_NOT_FOUND)
    }

    @After
    fun cleanup() {
        Dispatchers.resetMain()
    }

    private fun buildRequestManager(
        prepareTask: Task<StandardIntegrityTokenProvider>
    ) = IntegrityStandardRequestManager(
        cloudProjectNumber = 123456789L,
        logError = { _, _ -> },
        factory = FakeStandardIntegrityManagerFactory(prepareTask)
    )
}

class FakeStandardIntegrityManagerFactory(
    private val prepareTask: Task<StandardIntegrityTokenProvider>
) : StandardIntegrityManagerFactory {
    override fun create(): StandardIntegrityManager =
        StandardIntegrityManager { prepareTask }
}

class FakeStandardIntegrityTokenProvider(
    private val requestTask: Task<StandardIntegrityToken>
) : StandardIntegrityTokenProvider {
    override fun request(request: StandardIntegrityTokenRequest): Task<StandardIntegrityToken> {
        return requestTask
    }
}

class FakeStandardIntegrityToken : StandardIntegrityToken() {
    override fun showDialog(p0: Activity?, p1: Int) = Tasks.forResult(0)
    override fun token(): String = "123456789"
}
