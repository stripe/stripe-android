package com.stripe.android.core.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.APIException
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
            executeRequest = { throw APIException() },
            expectedResult = ListenableWorker.Result.failure(),
        )
    }

    private suspend fun runWorkerTest(
        executeRequest: () -> StripeResponse<String>,
        expectedResult: ListenableWorker.Result,
    ) {
        val request = mockAnalyticsRequest()
        val input = SendAnalyticsRequestV2Worker.createInputData(request)

        val worker = TestListenableWorkerBuilder<SendAnalyticsRequestV2Worker>(application)
            .setInputData(input)
            .build()

        val networkClient = FakeStripeNetworkClient(executeRequest = executeRequest)

        SendAnalyticsRequestV2Worker.setNetworkClient(networkClient)

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
