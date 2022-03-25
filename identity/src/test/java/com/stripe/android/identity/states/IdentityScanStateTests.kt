package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
class IdentityScanStateTests {

    private val mockNeverTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(false)
    }

    private val mockAlwaysTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(true)
    }

    @Test
    fun `Initial can't transition with unmatched AnalyzerOutput`() {
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.ID_FRONT,
            mockNeverTimeoutClockMark
        )
        val resultState = initialState.consumeTransition(ID_BACK_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Initial transitions to Found with matched AnalyzerOutput`() {
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.ID_FRONT,
            mockNeverTimeoutClockMark
        )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Found::class.java)
    }

    @Test
    fun `Found transitions to Unsatisfied with bad hit rate`() {
        val mockTargetState = mock<IdentityScanState>()
        val mockTransitioner = mock<IdentityFoundStateTransitioner>().also {
            whenever(it.transition(any(), any())).thenReturn(mockTargetState)
        }

        val initialState =
            IdentityScanState.Found(
                IdentityScanState.ScanType.ID_FRONT,
                mockNeverTimeoutClockMark,
                mockTransitioner
            )
        assertSame(initialState.consumeTransition(ID_FRONT_OUTPUT), mockTargetState)
    }

    @Test
    fun `Satisfied transitions to Satisfied before timeout`() {
        val mockReachAtClockMark: ClockMark = mock()
        whenever(mockReachAtClockMark.elapsedSince()).thenReturn(DURATION_BEFORE_TIMEOUT)

        val initialState =
            IdentityScanState.Satisfied(
                IdentityScanState.ScanType.ID_FRONT,
                mockNeverTimeoutClockMark,
                mockReachAtClockMark
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
            mockReachAtClockMark
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
                mockReachAtClockMark
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
            mockReachAtClockMark
        )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Initial::class.java)
        assertThat(resultState.type).isEqualTo(initialScanType)
    }

    @Test
    fun `Initial times out`() {
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.ID_FRONT,
            mockAlwaysTimeoutClockMark
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
        val ID_FRONT_OUTPUT = AnalyzerOutput(
            BoundingBox(0f, 0f, 0f, 0f),
            Category.ID_FRONT,
            0f,
            listOf()
        )
        val ID_BACK_OUTPUT = AnalyzerOutput(
            BoundingBox(0f, 0f, 0f, 0f),
            Category.ID_BACK,
            0f,
            listOf()
        )
        val DURATION_BEFORE_TIMEOUT = 499.milliseconds
        val DURATION_AFTER_TIMEOUT = 501.milliseconds
    }
}
