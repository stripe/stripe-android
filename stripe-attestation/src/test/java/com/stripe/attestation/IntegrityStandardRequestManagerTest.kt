package com.stripe.attestation

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.integrity.StandardIntegrityManager
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityToken
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenProvider
import com.google.android.play.core.integrity.StandardIntegrityManager.StandardIntegrityTokenRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import kotlin.test.Test

class IntegrityStandardRequestManagerTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun `prepare - success`() = runTest {
        val tokenProvider = FakeStandardIntegrityTokenProvider(Tasks.forResult(FakeStandardIntegrityToken()))
        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forResult(tokenProvider),
        )

        val result = integrityStandardRequestManager.prepare()

        assert(result.isSuccess)
    }

    @Test
    fun `prepare - failure`() = runTest {
        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forException(Exception("Failed to build token provider")),
        )

        val result = integrityStandardRequestManager.prepare()

        assert(result.isFailure)
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
    fun `requestToken - failure`() = runTest {
        val tokenProvider = FakeStandardIntegrityTokenProvider(Tasks.forException(Exception("Failed to request token")))
        val integrityStandardRequestManager = buildRequestManager(
            prepareTask = Tasks.forResult(tokenProvider),
        )

        integrityStandardRequestManager.prepare()
        val result = integrityStandardRequestManager.requestToken("requestIdentifier")

        assert(result.isFailure)
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
