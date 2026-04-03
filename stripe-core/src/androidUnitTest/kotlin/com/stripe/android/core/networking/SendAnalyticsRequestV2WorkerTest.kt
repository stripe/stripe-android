package com.stripe.android.core.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.utils.FakeAnalyticsRequestV2Storage
import com.stripe.android.core.utils.FakeStripeNetworkClient
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class SendAnalyticsRequestV2WorkerTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `Returns success upon successful network response`() = runTest {
        runWorkerTest(
            executeRequest = { StripeResponse(200, body = null) },
            expectedResult = ListenableWorker.Result.success(),
        )
    }

    @Test
    fun `Returns retry upon retryable network response`() = runTest {
        runWorkerTest(
            executeRequest = { throw APIConnectionException() },
            expectedResult = ListenableWorker.Result.retry(),
        )
    }

    @Test
    fun `Returns failure upon network response that can't be retried`() = runTest {
        runWorkerTest(
            executeRequest = { throw InvalidRequestException() },
            expectedResult = ListenableWorker.Result.failure(),
        )
    }

    @Test
    fun `Returns failure upon exceeding max attempts`() = runTest {
        runWorkerTest(
            executeRequest = { throw APIConnectionException() },
            expectedResult = ListenableWorker.Result.failure(),
            currentRunAttempt = 4,
        )
    }

    private suspend fun runWorkerTest(
        executeRequest: () -> StripeResponse<String>,
        expectedResult: ListenableWorker.Result,
        currentRunAttempt: Int = 0,
    ) {
        val networkClient = FakeStripeNetworkClient(executeRequest = executeRequest)
        SendAnalyticsRequestV2Worker.setNetworkClient(networkClient)

        val storage = FakeAnalyticsRequestV2Storage()
        SendAnalyticsRequestV2Worker.setStorage(storage)

        val request = mockAnalyticsRequest()
        val id = storage.store(request)
        val input = SendAnalyticsRequestV2Worker.createInputData(id)

        val worker = TestListenableWorkerBuilder<SendAnalyticsRequestV2Worker>(application)
            .setInputData(input)
            .setRunAttemptCount(currentRunAttempt)
            .build()

        val result = worker.doWork()
        assertThat(result).isEqualTo(expectedResult)
    }

    private fun mockAnalyticsRequest(): AnalyticsRequestV2 {
        return AnalyticsRequestV2.create(
            eventName = "event_name",
            clientId = "123",
            origin = "origin",
            params = emptyMap<String, String>(),
        )
    }
}
