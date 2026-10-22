package com.stripe.android.paymentsheet.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

internal class PaymentSheetFirstFrameTimerTest {
    @Test
    fun `drawing before loading completes does not consume the sample`() = runScenario {
        assertThat(timer.takeDuration()).isNull()

        timer.onLoadComplete()
        now += 20.milliseconds.inWholeNanoseconds

        assertThat(timer.takeDuration()).isEqualTo(20.milliseconds)
    }

    @Test
    fun `duration excludes loading and retains submillisecond precision`() = runScenario {
        now += 1_000.milliseconds.inWholeNanoseconds
        timer.onLoadComplete()
        now += 16.milliseconds.inWholeNanoseconds + 500_000

        assertThat(timer.takeDuration()).isEqualTo(16_500_000.nanoseconds)
    }

    @Test
    fun `subsequent draws do not produce another sample`() = runScenario {
        timer.onLoadComplete()
        now += 20.milliseconds.inWholeNanoseconds
        assertThat(timer.takeDuration()).isEqualTo(20.milliseconds)

        now += 20.milliseconds.inWholeNanoseconds

        assertThat(timer.takeDuration()).isNull()
    }

    @Test
    fun `presentations have independent timers`() = runScenario {
        val otherTimer = PaymentSheetFirstFrameTimer { now }
        timer.onLoadComplete()
        now += 10.milliseconds.inWholeNanoseconds
        otherTimer.onLoadComplete()
        now += 20.milliseconds.inWholeNanoseconds

        assertThat(timer.takeDuration()).isEqualTo(30.milliseconds)
        assertThat(otherTimer.takeDuration()).isEqualTo(20.milliseconds)
    }

    private fun runScenario(block: Scenario.() -> Unit) {
        Scenario().block()
    }

    private class Scenario {
        var now = 0L
        val timer = PaymentSheetFirstFrameTimer { now }
    }
}
