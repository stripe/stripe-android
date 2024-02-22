package com.stripe.android.core.networking

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.Logger
import com.stripe.android.core.utils.FakeStripeNetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class DefaultAnalyticsRequestV2ExecutorTest {

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun before() {
        initializeWorkManager()
    }

    @Test
    fun `Enqueues requests directly if WorkManager is available`() = runTest(testDispatcher) {
        val networkClient = FakeStripeNetworkClient()

        val executor = DefaultAnalyticsRequestV2Executor(
            application = application,
            networkClient = networkClient,
            logger = Logger.noop(),
            isWorkManagerAvailable = { true },
            dispatcher = testDispatcher,
        )

        val request = mockAnalyticsRequest()
        executor.enqueue(request)

        val work = findWork()
        assertThat(work?.state).isEqualTo(WorkInfo.State.ENQUEUED)

        assertThat(networkClient.executeRequestCalled).isFalse()
    }

    @Test
    fun `Executes requests directly if WorkManager isn't available`() = runTest(testDispatcher) {
        val networkClient = FakeStripeNetworkClient()

        val executor = DefaultAnalyticsRequestV2Executor(
            application = application,
            networkClient = networkClient,
            logger = Logger.noop(),
            isWorkManagerAvailable = { false },
            dispatcher = testDispatcher,
        )

        val request = mockAnalyticsRequest()
        executor.enqueue(request)

        assertThat(networkClient.executeRequestCalled).isTrue()
    }

    private fun initializeWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(application)
    }

    private fun mockAnalyticsRequest(): AnalyticsRequestV2 {
        return AnalyticsRequestV2.create(
            eventName = "event_name",
            clientId = "123",
            origin = "origin",
            params = emptyMap<String, String>(),
        )
    }

    private suspend fun findWork(): WorkInfo? {
        val workManager = WorkManager.getInstance(application)
        val tag = SendAnalyticsRequestV2Worker.TAG

        return withContext(Dispatchers.IO) {
            workManager.getWorkInfosByTag(tag).get().singleOrNull()
        }
    }
}
