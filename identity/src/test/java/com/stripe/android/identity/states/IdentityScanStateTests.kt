package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.states.IdentityScanState.Found.Companion.FRAMES_REQUIRED
import com.stripe.android.identity.states.IdentityScanState.Found.Companion.HITS_REQUIRED
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IdentityScanStateTests {

    @Test
    fun `Initial can't transition with unmatched AnalyzerOutput`() {
        val initialState = IdentityScanState.Initial(IdentityScanState.ScanType.ID_FRONT)
        val resultState = initialState.consumeTransition(ID_BACK_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Initial transitions to Found with matched AnalyzerOutput`() {
        val initialState = IdentityScanState.Initial(IdentityScanState.ScanType.ID_FRONT)
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Found::class.java)
    }

    @Test
    fun `Found transitions to Unsatisfied with bad hit rate`() {
        val badHitCount = HITS_REQUIRED - 10

        val initialState = IdentityScanState.Found(IdentityScanState.ScanType.ID_FRONT).also {
            // hits count below required
            it.hitsCount = badHitCount
            for (i in 1..FRAMES_REQUIRED) {
                it.results.addLast(true)
            }
        }
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        assertThat((resultState as IdentityScanState.Unsatisfied).reason).isEqualTo(
            "hits count below expected: $badHitCount"
        )
    }

    @Test
    fun `Found transitions to Satisfied with good hit rate`() {
        val goodHitCount = HITS_REQUIRED + 10

        val initialState = IdentityScanState.Found(IdentityScanState.ScanType.ID_FRONT).also {
            // hits count below required
            it.hitsCount = goodHitCount
            for (i in 1..FRAMES_REQUIRED) {
                it.results.addLast(true)
            }
        }
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
    }

    @Test
    fun `Found transitions to Found when more results are required`() {
        val initialState = IdentityScanState.Found(IdentityScanState.ScanType.ID_FRONT).also {
            // hits count below required
            for (i in 1..(FRAMES_REQUIRED - 10)) {
                it.results.addLast(true)
            }
        }
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Satisfied transitions to Satisfied before timeout`() {
        val mockClockMark: ClockMark = mock()
        whenever(mockClockMark.elapsedSince()).thenReturn(DURATION_BEFORE_TIMEOUT)

        val initialState =
            IdentityScanState.Satisfied(IdentityScanState.ScanType.ID_FRONT, mockClockMark)
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Satisfied transitions to Finished after timeout`() {
        val mockClockMark: ClockMark = mock()
        whenever(mockClockMark.elapsedSince()).thenReturn(DURATION_AFTER_TIMEOUT)

        val initialScanType = IdentityScanState.ScanType.ID_FRONT
        val initialState = IdentityScanState.Satisfied(initialScanType, mockClockMark)
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Finished::class.java)
        assertThat(resultState.type).isEqualTo(initialScanType)
    }

    @Test
    fun `Unsatisfied transitions to Unsatisfied before timeout`() {
        val mockClockMark: ClockMark = mock()
        whenever(mockClockMark.elapsedSince()).thenReturn(DURATION_BEFORE_TIMEOUT)

        val initialState =
            IdentityScanState.Unsatisfied(
                "reason",
                IdentityScanState.ScanType.ID_FRONT,
                mockClockMark
            )
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    @Test
    fun `Unsatisfied transitions to Initial after timeout`() {
        val mockClockMark: ClockMark = mock()
        whenever(mockClockMark.elapsedSince()).thenReturn(DURATION_AFTER_TIMEOUT)

        val initialScanType = IdentityScanState.ScanType.ID_FRONT
        val initialState = IdentityScanState.Unsatisfied("reason", initialScanType, mockClockMark)
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isInstanceOf(IdentityScanState.Initial::class.java)
        assertThat(resultState.type).isEqualTo(initialScanType)
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
