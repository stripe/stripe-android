package com.stripe.android.polling

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent.Status.RequiresAction
import com.stripe.android.model.StripeIntent.Status.RequiresCapture
import com.stripe.android.model.StripeIntent.Status.Succeeded
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultIntentStatusPollerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val delayTimeInMillis = 1010L // 1 second + 10ms offset (so that we run after polling completes)

    @Test
    fun `Updates state when polling result changes`() = runTest(testDispatcher) {
        val statuses = listOf(
            RequiresAction,
            RequiresCapture,
            Succeeded,
        )

        val poller = createIntentStatusPoller(
            enqueuedStatuses = statuses,
            dispatcher = testDispatcher
        )

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        advanceTimeBy(delayTimeInMillis)
        assertThat(poller.state.value).isEqualTo(RequiresCapture)

        advanceTimeBy(delayTimeInMillis)
        assertThat(poller.state.value).isEqualTo(Succeeded)

        poller.stopPolling()
    }

    @Test
    fun `Updates state when polling result changes - fixed intervals`() = runTest(testDispatcher) {
        val statuses = listOf(
            RequiresAction,
            RequiresCapture,
            Succeeded,
        )
        val poller = createIntentStatusPoller(
            enqueuedStatuses = statuses,
            dispatcher = testDispatcher,
        )

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        advanceTimeBy(delayTimeInMillis)
        assertThat(poller.state.value).isEqualTo(RequiresCapture)

        advanceTimeBy(delayTimeInMillis)
        assertThat(poller.state.value).isEqualTo(Succeeded)

        poller.stopPolling()
    }

    @Test
    fun `Stops polling when intent reaches terminal state`() = runTest(testDispatcher) {
        val poller = createIntentStatusPoller(
            enqueuedStatuses = listOf(
                Succeeded,
                RequiresAction,
            ),
            dispatcher = testDispatcher,
        )

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(Succeeded)

        advanceTimeBy(delayTimeInMillis)

        // The state should be unchanged
        assertThat(poller.state.value).isEqualTo(Succeeded)
    }

    @Test
    fun `Canceling polling makes poller not emit any more states`() = runTest(testDispatcher) {
        val poller = createIntentStatusPoller(
            enqueuedStatuses = listOf(
                RequiresAction,
                Succeeded,
            ),
            dispatcher = testDispatcher
        )

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        // Advance partially into the current delay
        advanceTimeBy(1_000L)

        poller.stopPolling()

        // A random large delay to show that no other status was emitted
        // while polling is stopped
        advanceTimeBy(10_000L)

        // The state should be unchanged
        assertThat(poller.state.value).isEqualTo(RequiresAction)
    }

    @Test
    fun `Pausing makes poller not emit any more states until resumed`() = runTest(testDispatcher) {
        val poller = createIntentStatusPoller(
            enqueuedStatuses = listOf(
                RequiresAction,
                Succeeded
            ),
            dispatcher = testDispatcher
        )

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        // Advance partially into the current delay
        advanceTimeBy(1_000L)

        poller.stopPolling()

        // A random large delay to show that no other status was emitted
        // while polling is stopped
        advanceTimeBy(10_000L)

        // The state should be unchanged
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(Succeeded)

        poller.stopPolling()
    }
}
