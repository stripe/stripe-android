package com.stripe.android.core.networking

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.networking.DefaultAnalyticsRequestExecutor.Companion.FIELD_EVENT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DefaultAnalyticsRequestExecutorTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val stripeNetworkClient: StripeNetworkClient = mock()

    private val analyticsRequestExecutor = DefaultAnalyticsRequestExecutor(
        context = context,
        stripeNetworkClient = stripeNetworkClient,
        workContext = testDispatcher,
    )

    private val analyticsRequest = AnalyticsRequest(mapOf(FIELD_EVENT to TEST_EVENT), emptyMap())

    @Test
    fun `Should delegate to network client if WorkManager is not available`() = runTest {
        analyticsRequestExecutor.executeAsync(analyticsRequest)
        verify(stripeNetworkClient).executeRequest(same(analyticsRequest))
    }

    @Test
    fun `Should use WorkManager if available`() = runTest {
        initializeWorkManager()
        analyticsRequestExecutor.executeAsync(analyticsRequest)

        val work = findWork()
        assertThat(work).isNotNull()
        assertThat(work?.state).isEqualTo(WorkInfo.State.ENQUEUED)
    }

    private fun initializeWorkManager() {
        val config = Configuration.Builder().build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    private suspend fun findWork(): WorkInfo? {
        val workManager = WorkManager.getInstance(context)
        val tag = DefaultAnalyticsRequestExecutor.workerTag

        return withContext(Dispatchers.IO) {
            workManager.getWorkInfosByTag(tag).get().singleOrNull()
        }
    }

    private companion object {
        const val TEST_EVENT = "TEST_EVENT"
    }
}
