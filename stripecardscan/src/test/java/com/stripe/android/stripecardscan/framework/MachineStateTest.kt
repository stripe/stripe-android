package com.stripe.android.stripecardscan.framework

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class MachineStateTest {

    private class TestState(timeSource: kotlin.time.TimeSource) : MachineState(timeSource) {
        // Expose reachedStateAt for testing
        val stateReachedAt get() = reachedStateAt
    }

    @Test
    fun `reachedStateAt is set on construction`() {
        val timeSource = TestTimeSource()

        val state = TestState(timeSource)

        // elapsed should be zero immediately after creation
        assertThat(state.stateReachedAt.elapsedNow()).isEqualTo(kotlin.time.Duration.ZERO)
    }

    @Test
    fun `elapsed time increases after construction`() {
        val timeSource = TestTimeSource()
        val state = TestState(timeSource)

        timeSource += 5.seconds

        assertThat(state.stateReachedAt.elapsedNow()).isEqualTo(5.seconds)
    }

    @Test
    fun `timeSource is accessible`() {
        val timeSource = TestTimeSource()
        val state = TestState(timeSource)

        assertThat(state.timeSource).isSameInstanceAs(timeSource)
    }
}
