package com.stripe.android.polling

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.StripeIntent.Status.RequiresAction
import com.stripe.android.model.StripeIntent.Status.RequiresCapture
import com.stripe.android.model.StripeIntent.Status.RequiresConfirmation
import com.stripe.android.model.StripeIntent.Status.RequiresPaymentMethod
import com.stripe.android.model.StripeIntent.Status.Succeeded
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultIntentStatusPollerTest {

    private val testDispatcher = UnconfinedTestDispatcher()

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

        val nextDelay = exponentialDelayProvider()

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        advanceTimeBy(nextDelay())
        assertThat(poller.state.value).isEqualTo(RequiresCapture)

        advanceTimeBy(nextDelay())
        assertThat(poller.state.value).isEqualTo(Succeeded)

        poller.stopPolling()
    }

    @Test
    fun `Stops when reaching max attempts`() = runTest(testDispatcher) {
        val poller = createIntentStatusPoller(
            maxAttempts = 3,
            enqueuedStatuses = listOf(
                RequiresPaymentMethod,
                RequiresConfirmation,
                RequiresAction,
                Succeeded,
            ),
            dispatcher = testDispatcher,
        )

        val nextDelay = exponentialDelayProvider()

        assertThat(poller.state.value).isNull()

        poller.startPolling(scope = this@runTest)
        assertThat(poller.state.value).isEqualTo(RequiresPaymentMethod)

        advanceTimeBy(nextDelay())
        assertThat(poller.state.value).isEqualTo(RequiresConfirmation)

        advanceTimeBy(nextDelay())
        assertThat(poller.state.value).isEqualTo(RequiresAction)

        advanceTimeBy(nextDelay())

        // The state should be unchanged
        assertThat(poller.state.value).isEqualTo(RequiresAction)
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
