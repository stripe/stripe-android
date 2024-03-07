package com.stripe.android.financialconnections.exception

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.testing.TestLifecycleOwner
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.exception.APIConnectionException
import com.stripe.android.core.exception.StripeException
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsEvent
import com.stripe.android.financialconnections.analytics.FinancialConnectionsAnalyticsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.lang.Thread.UncaughtExceptionHandler

@OptIn(ExperimentalCoroutinesApi::class)
internal class FinancialConnectionsErrorHandlerTest {

    @Before
    fun before() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Correctly consumes exceptions when merchant has uncaught exception handler`() = runTest {
        val merchantHandler = RecordingUncaughtExceptionHandler()
        val financialConnectionsHandler = RecordingFinancialConnectionsErrorHandler()

        Thread.setDefaultUncaughtExceptionHandler(merchantHandler)

        val lifecycleOwner = TestLifecycleOwner()
        financialConnectionsHandler.handler.setup(lifecycleOwner)

        mockUncaughtException()

        assertThat(merchantHandler.encounteredErrors).hasSize(1)
        assertThat(financialConnectionsHandler.encounteredErrors).hasSize(1)

        lifecycleOwner.setCurrentState(Lifecycle.State.DESTROYED)

        mockUncaughtException()

        assertThat(merchantHandler.encounteredErrors).hasSize(2)
        assertThat(financialConnectionsHandler.encounteredErrors).hasSize(1)
    }

    @Test
    fun `Correctly consumes exceptions when merchant does not have uncaught exception handler`() = runTest {
        val financialConnectionsHandler = RecordingFinancialConnectionsErrorHandler()
        Thread.setDefaultUncaughtExceptionHandler(null)

        val lifecycleOwner = TestLifecycleOwner()
        financialConnectionsHandler.handler.setup(lifecycleOwner)

        mockUncaughtException()
        assertThat(financialConnectionsHandler.encounteredErrors).hasSize(1)

        lifecycleOwner.setCurrentState(Lifecycle.State.DESTROYED)

        // The FinancialConnectionsErrorHandler no longer receives uncaught exceptions
        mockUncaughtException()
        assertThat(financialConnectionsHandler.encounteredErrors).hasSize(1)
    }

    private fun mockUncaughtException() {
        Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(
            Thread.currentThread(),
            StripeException.create(APIConnectionException()),
        )
    }
}

private class RecordingUncaughtExceptionHandler : UncaughtExceptionHandler {

    private val _encounteredErrors = mutableListOf<Throwable>()

    val encounteredErrors: List<Throwable>
        get() = _encounteredErrors

    override fun uncaughtException(thread: Thread, error: Throwable) {
        _encounteredErrors += error
    }
}

private class RecordingFinancialConnectionsErrorHandler {

    private val trackedEvents = mutableListOf<FinancialConnectionsAnalyticsEvent>()
    private val tracker = FinancialConnectionsAnalyticsTracker(trackedEvents::add)

    val handler = FinancialConnectionsErrorHandler(tracker)

    val encounteredErrors: List<FinancialConnectionsAnalyticsEvent>
        get() = trackedEvents
}
