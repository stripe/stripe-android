package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.identity.ml.AnalyzerOutput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class LookBackWindowTransitionerTest {

    private val mockNeverTimeOutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(false)
    }

    @Test
    fun `transitions to Unsatisfied with bad hit rate`() {
        val transitioner = LookBackWindowTransitioner()

        val badHitCount = LookBackWindowTransitioner.DEFAULT_HITS_REQUIRED - 10
        transitioner.hitsCount = badHitCount
        for (i in 1..LookBackWindowTransitioner.DEFAULT_FRAMES_REQUIRED) {
            transitioner.results.addLast(true)
        }

        val resultState = transitioner.transition(
            mock<IdentityScanState.Found>().also {
                whenever(it.type).thenReturn(IdentityScanState.ScanType.ID_FRONT)
                whenever(it.timeoutAt).thenReturn(mockNeverTimeOutClockMark)
                whenever(it.transitioner).thenReturn(transitioner)
            },
            ID_FRONT_OUTPUT
        )

        assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        assertThat((resultState as IdentityScanState.Unsatisfied).reason).isEqualTo(
            "hits count below expected: $badHitCount"
        )
    }

    @Test
    fun `transitions to Satisfied with good hit rate`() {
        val transitioner = LookBackWindowTransitioner()

        val goodHitCount = LookBackWindowTransitioner.DEFAULT_HITS_REQUIRED + 10
        transitioner.hitsCount = goodHitCount
        for (i in 1..LookBackWindowTransitioner.DEFAULT_FRAMES_REQUIRED) {
            transitioner.results.addLast(true)
        }

        val resultState = transitioner.transition(
            mock<IdentityScanState.Found>().also {
                whenever(it.type).thenReturn(IdentityScanState.ScanType.ID_FRONT)
                whenever(it.timeoutAt).thenReturn(mockNeverTimeOutClockMark)
                whenever(it.transitioner).thenReturn(transitioner)
            },
            ID_FRONT_OUTPUT
        )

        assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
    }

    @Test
    fun `transitions to Found when more results are required`() {
        val initialState = IdentityScanState.Found(
            IdentityScanState.ScanType.ID_FRONT,
            mockNeverTimeOutClockMark,
            LookBackWindowTransitioner()
        ).also {
            // hits count below required
            for (i in 1..(LookBackWindowTransitioner.DEFAULT_FRAMES_REQUIRED - 10)) {
                (it.transitioner as LookBackWindowTransitioner).results.addLast(true)
            }
        }
        val resultState = initialState.consumeTransition(ID_FRONT_OUTPUT)

        assertThat(resultState).isSameInstanceAs(initialState)
    }

    private companion object {
        val ID_FRONT_OUTPUT = AnalyzerOutput(
            BoundingBox(0f, 0f, 0f, 0f),
            Category.ID_FRONT,
            0f,
            listOf()
        )
    }
}
