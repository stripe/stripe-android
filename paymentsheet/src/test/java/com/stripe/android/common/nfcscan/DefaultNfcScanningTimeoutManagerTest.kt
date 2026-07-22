package com.stripe.android.common.nfcscan

import app.cash.turbine.test
import com.stripe.android.testing.CleanupTestRule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

internal class DefaultNfcScanningTimeoutManagerTest {
    @get:Rule
    val coroutineScopeCleanupRule = CleanupTestRule<CoroutineScope> { cancel() }

    @Test
    fun `timeout emits after inactivity duration`() = runTest {
        val timeoutManager = createTimeoutManager()

        timeoutManager.timeout.test {
            timeoutManager.start()
            advanceUntilIdle()

            advanceTimeBy(INACTIVITY_TIMEOUT)
            advanceUntilIdle()

            awaitItem()
            ensureAllEventsConsumed()
        }
    }

    private fun TestScope.createTimeoutManager(): DefaultNfcScanningTimeoutManager {
        return DefaultNfcScanningTimeoutManager(
            coroutineScope = coroutineScopeCleanupRule.track(
                CoroutineScope(StandardTestDispatcher(testScheduler))
            ),
        )
    }

    private companion object {
        val INACTIVITY_TIMEOUT = 20.seconds
    }
}
