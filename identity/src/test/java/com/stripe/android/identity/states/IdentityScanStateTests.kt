package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityScanStateTests {

    private val mockNeverTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(false)
    }

    private val mockAlwaysTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(true)
    }

    private val mockTransitioner = mock<IdentityScanStateTransitioner>()

    @Test
    fun `Initial transitions to Timeout if timeOut passed`() {
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.ID_FRONT,
            mockAlwaysTimeoutClockMark,
            mockTransitioner
        )
        val resultState = initialState.consumeTransition(ID_BACK_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Initial calls transitionFromInitial if not timeout`() {
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.ID_FRONT,
            mockNeverTimeoutClockMark,
            mockTransitioner
        )
        initialState.consumeTransition(ID_FRONT_OUTPUT)

        verify(mockTransitioner).transitionFromInitial(same(initialState), same(ID_FRONT_OUTPUT))
    }

    @Test
    fun `Found transitions to Timeout if timeOut passed`() {
        val mockTargetState = mock<IdentityScanState>()

        val initialState =
            IdentityScanState.Found(
                IdentityScanState.ScanType.ID_FRONT,
                mockAlwaysTimeoutClockMark,
                mockTransitioner
            )
        val resultState = initialState.consumeTransition(ID_BACK_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Found calls transitionFromFound if not timeout`() {
        val initialState = IdentityScanState.Found(
            IdentityScanState.ScanType.ID_FRONT,
            mockNeverTimeoutClockMark,
            mockTransitioner
        )
        initialState.consumeTransition(ID_FRONT_OUTPUT)

        verify(mockTransitioner).transitionFromFound(same(initialState), same(ID_FRONT_OUTPUT))
    }

    @Test
    fun `Satisfied transitions to Satisfied before timeout`() {
        val mockReachAtClockMark: ClockMark = mock()
        whenever(mockReachAtClockMark.elapsedSince()).thenReturn(DURATION_BEFORE_TIMEOUT)

        val initialState =
            IdentityScanState.Satisfied(
                IdentityScanState.ScanType.ID_FRONT,
                mockNeverTimeoutClockMark,
                mockTransitioner,
                mockReachAtClockMark,
                displaySatisfiedDuration = TIME_OUT_IN_MILLIS
            )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Satisfied transitions to Finished after timeout`() {
        val mockReachAtClockMark: ClockMark = mock()
        whenever(mockReachAtClockMark.elapsedSince()).thenReturn(DURATION_AFTER_TIMEOUT)

        val initialScanType = IdentityScanState.ScanType.ID_FRONT
        val initialState = IdentityScanState.Satisfied(
            initialScanType,
            mockNeverTimeoutClockMark,
            mockTransitioner,
            mockReachAtClockMark,
            displaySatisfiedDuration = TIME_OUT_IN_MILLIS
        )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Finished::class.java)
        assertThat(resultState.type).isEqualTo(initialScanType)
    }

    @Test
    fun `Unsatisfied transitions to Unsatisfied before timeout`() {
        val mockReachAtClockMark: ClockMark = mock()
        whenever(mockReachAtClockMark.elapsedSince()).thenReturn(DURATION_BEFORE_TIMEOUT)
        val initialState =
            IdentityScanState.Unsatisfied(
                "reason",
                IdentityScanState.ScanType.ID_FRONT,
                mockNeverTimeoutClockMark,
                mockTransitioner,
                mockReachAtClockMark,
                displayUnsatisfiedDuration = TIME_OUT_IN_MILLIS
            )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Unsatisfied transitions to Initial after timeout`() {
        val mockReachAtClockMark: ClockMark = mock()
        whenever(mockReachAtClockMark.elapsedSince()).thenReturn(DURATION_AFTER_TIMEOUT)

        val initialScanType = IdentityScanState.ScanType.ID_FRONT
        val initialState = IdentityScanState.Unsatisfied(
            "reason",
            initialScanType,
            mockNeverTimeoutClockMark,
            mockTransitioner,
            mockReachAtClockMark,
            displayUnsatisfiedDuration = TIME_OUT_IN_MILLIS
        )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Initial::class.java)
        assertThat(resultState.type).isEqualTo(initialScanType)
    }

    @Test
    fun `Initial times out`() {
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.ID_FRONT,
            mockAlwaysTimeoutClockMark,
            mockTransitioner
        )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Found times out`() {
        val initialState =
            IdentityScanState.Found(
                IdentityScanState.ScanType.ID_FRONT,
                mockAlwaysTimeoutClockMark,
                mock()
            )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Unsatisfied times out`() {
        val initialState =
            IdentityScanState.Unsatisfied(
                "reason",
                IdentityScanState.ScanType.ID_FRONT,
                mockAlwaysTimeoutClockMark,
                mock()
            )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    private companion object {
        val ID_FRONT_OUTPUT = IDDetectorOutput(
            BoundingBox(0f, 0f, 0f, 0f),
            Category.ID_FRONT,
            0f,
            listOf()
        )
        val ID_BACK_OUTPUT = IDDetectorOutput(
            BoundingBox(0f, 0f, 0f, 0f),
            Category.ID_BACK,
            0f,
            listOf()
        )
        const val TIME_OUT_IN_MILLIS = 500
        val DURATION_BEFORE_TIMEOUT = (TIME_OUT_IN_MILLIS - 1).milliseconds
        val DURATION_AFTER_TIMEOUT = (TIME_OUT_IN_MILLIS + 1).milliseconds
    }
}
