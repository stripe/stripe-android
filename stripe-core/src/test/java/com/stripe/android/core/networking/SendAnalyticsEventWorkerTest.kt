package com.stripe.android.core.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.InvalidRequestException
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor.Companion.FIELD_EVENT
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor.SendAnalyticsEventWorker
import com.stripe.android.core.utils.FakeStripeNetworkClient
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class SendAnalyticsEventWorkerTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val networkClient = FakeStripeNetworkClient()

    @Before
    fun before() {
        WorkManagerHelpers.setNetworkClient(networkClient)
    }

    @Test
    fun `Returns success if analytics request succeeds`() = runTest {
        val worker = createWorker()
        enqueueSuccessResponse()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.success())
    }

    @Test
    fun `Returns retry if analytics request fails due to fixable error reason`() = runTest {
        val worker = createWorker()
        enqueueRetryResponse()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.retry())
    }

    @Test
    fun `Returns failure if analytics request fails due to non-fixable error reason`() = runTest {
        val worker = createWorker()
        enqueueFailureResponse()

        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    @Test
    fun `Returns failure if analytics request can't be deserialized`() = runTest {
        val worker = createWorker(request = null)
        val result = worker.doWork()
        assertThat(result).isEqualTo(ListenableWorker.Result.failure())
    }

    private fun createWorker(
        request: AnalyticsRequest? = defaultAnalyticsRequest(),
    ): SendAnalyticsEventWorker {
        val data = if (request != null) {
            SendAnalyticsEventWorker.createInputData(request)
        } else {
            Data.EMPTY
        }

        return TestListenableWorkerBuilder<SendAnalyticsEventWorker>(
            context = context,
        ).setInputData(data).build()
    }

    private fun enqueueSuccessResponse() {
        val response = StripeResponse<String>(code = 200, body = null)
        networkClient.enqueueResult(Result.success(response))
    }

    private fun enqueueRetryResponse() {
        val response = APIConnectionException()
        networkClient.enqueueResult(Result.failure(response))
    }

    private fun enqueueFailureResponse() {
        val response = InvalidRequestException()
        networkClient.enqueueResult(Result.failure(response))
    }

    private fun defaultAnalyticsRequest(): AnalyticsRequest {
        return AnalyticsRequest(
            params = mapOf(FIELD_EVENT to "test_event"),
            headers = emptyMap(),
        )
    }
}
