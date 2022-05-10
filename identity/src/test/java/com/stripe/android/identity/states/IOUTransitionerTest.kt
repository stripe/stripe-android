package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.IdentityScanState.ScanType
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class IOUTransitionerTest {
    private val mockNeverTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(false)
    }
    private val mockReachedStateAt = mock<ClockMark>().also {
        whenever(it.elapsedSince()).thenReturn(0.milliseconds)
    }

    @Test
    fun `transitions to Found when iOUCheckPass failed`() {
        val transitioner = IOUTransitioner()

        val foundState = IdentityScanState.Found(
            ScanType.ID_FRONT,
            mockNeverTimeoutClockMark,
            transitioner,
            mockReachedStateAt
        )
        // initialize previousBoundingBox
        transitioner.transition(foundState, INITIAL_ID_FRONT_OUTPUT)

        // send a low IOU result
        assertThat(
            transitioner.transition(
                foundState,
                createAnalyzerOutputWithLowIOU(INITIAL_ID_FRONT_OUTPUT)
            )
        ).isSameInstanceAs(foundState)

        // verify timer is reset
        assertThat(foundState.reachedStateAt).isNotSameInstanceAs(mockReachedStateAt)
    }

    @Test
    fun `transitions to Found when moreResultsRequired and Satisfied when timeRequired is met`() {
        val timeRequired = 500
        val transitioner = IOUTransitioner(timeRequired = timeRequired)

        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
            whenever(it.timeoutAt).thenReturn(mockNeverTimeoutClockMark)
            whenever(it.reachedStateAt).thenReturn(mockReachedStateAt)
            whenever(it.transitioner).thenReturn(transitioner)
        }

        val result = createAnalyzerOutputWithHighIOU(INITIAL_ID_FRONT_OUTPUT)

        // mock time required is not yet met
        whenever(mockReachedStateAt.elapsedSince()).thenReturn((timeRequired - 10).milliseconds)
        assertThat(
            transitioner.transition(
                mockFoundState,
                result
            )
        ).isSameInstanceAs(mockFoundState)

        // mock time required is met
        whenever(mockReachedStateAt.elapsedSince()).thenReturn((timeRequired + 10).milliseconds)
        val resultState = transitioner.transition(
            mockFoundState,
            createAnalyzerOutputWithHighIOU(result)
        )

        assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
        assertThat((resultState as IdentityScanState.Satisfied).type).isEqualTo(
            ScanType.ID_FRONT
        )
    }

    @Test
    fun `transitions to Found when moreResultsRequired and stays in Found when IOU check fails`() {
        val timeRequired = 500
        val allowedUnmatchedFrames = 2
        val transitioner = IOUTransitioner(
            timeRequired = timeRequired,
            allowedUnmatchedFrames = allowedUnmatchedFrames
        )

        // never meets required time
        whenever(mockReachedStateAt.elapsedSince()).thenReturn((timeRequired - 10).milliseconds)

        val foundState = IdentityScanState.Found(
            ScanType.ID_FRONT,
            mockNeverTimeoutClockMark,
            transitioner,
            mockReachedStateAt
        )

        // 1st frame - a match, stays in Found
        val result = createAnalyzerOutputWithHighIOU(INITIAL_ID_FRONT_OUTPUT)
        assertThat(
            transitioner.transition(
                foundState,
                result
            )
        ).isSameInstanceAs(foundState)

        // 2nd frame - a low IOU frame, stays in Found
        assertThat(
            transitioner.transition(
                foundState,
                createAnalyzerOutputWithLowIOU(result)
            )
        ).isSameInstanceAs(foundState)

        // verify timer is reset
        assertThat(foundState.reachedStateAt).isNotSameInstanceAs(mockReachedStateAt)
    }

    @Test
    fun `keeps in Found while unmatched frames within allowedUnmatchedFrames and to Unsatisfied when going beyond`() {
        val timeRequired = 500
        val allowedUnmatchedFrames = 2
        val transitioner = IOUTransitioner(
            timeRequired = timeRequired,
            allowedUnmatchedFrames = allowedUnmatchedFrames
        )

        // never meets required time
        whenever(mockReachedStateAt.elapsedSince()).thenReturn((timeRequired - 10).milliseconds)
        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
            whenever(it.timeoutAt).thenReturn(mockNeverTimeoutClockMark)
            whenever(it.reachedStateAt).thenReturn(mockReachedStateAt)
            whenever(it.transitioner).thenReturn(transitioner)
        }

        // 1st frame - a match, stays in Found
        var result = createAnalyzerOutputWithHighIOU(INITIAL_ID_FRONT_OUTPUT)
        assertThat(
            transitioner.transition(
                mockFoundState,
                result
            )
        ).isSameInstanceAs(mockFoundState)

        // follow up frames - high IOU frames with unmatch within allowedUnmatchedFrames, stays in Found
        for (i in 1..allowedUnmatchedFrames) {
            result = createAnalyzerOutputWithHighIOU(result, Category.ID_BACK)
            assertThat(
                transitioner.transition(
                    mockFoundState,
                    result
                )
            ).isSameInstanceAs(mockFoundState)
        }

        // another high iOU frame that breaks the streak
        val resultState = transitioner.transition(
            mockFoundState,
            createAnalyzerOutputWithHighIOU(result, Category.ID_BACK)
        )

        assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        assertThat((resultState as IdentityScanState.Unsatisfied).reason).isEqualTo(
            "Type ${Category.ID_BACK} doesn't match ${ScanType.ID_FRONT}",
        )
        assertThat(resultState.type).isEqualTo(ScanType.ID_FRONT)
    }

    @Test
    fun `keeps in Found while unmatched frames within allowedUnmatchedFrames and to Satisfied when going beyond`() {
        val timeRequired = 500
        val allowedUnmatchedFrames = 2
        val transitioner = IOUTransitioner(
            timeRequired = timeRequired,
            allowedUnmatchedFrames = allowedUnmatchedFrames
        )

        // never meets required time
        whenever(mockReachedStateAt.elapsedSince()).thenReturn((timeRequired - 10).milliseconds)
        val mockFoundState = mock<IdentityScanState.Found>().also {
            whenever(it.type).thenReturn(ScanType.ID_FRONT)
            whenever(it.timeoutAt).thenReturn(mockNeverTimeoutClockMark)
            whenever(it.reachedStateAt).thenReturn(mockReachedStateAt)
            whenever(it.transitioner).thenReturn(transitioner)
        }

        // 1st frame - a match, stays in Found
        var result = createAnalyzerOutputWithHighIOU(INITIAL_ID_FRONT_OUTPUT)
        assertThat(
            transitioner.transition(
                mockFoundState,
                result
            )
        ).isSameInstanceAs(mockFoundState)

        // follow up frames - high IOU frames with unmatch within allowedUnmatchedFrames, stays in Found
        for (i in 1..allowedUnmatchedFrames) {
            result = createAnalyzerOutputWithHighIOU(result, Category.ID_BACK)
            assertThat(
                transitioner.transition(
                    mockFoundState,
                    result
                )
            ).isSameInstanceAs(mockFoundState)
        }

        // mock required time is met
        whenever(mockReachedStateAt.elapsedSince()).thenReturn((timeRequired + 10).milliseconds)
        // another high iOU frame with a match
        val resultState = transitioner.transition(
            mockFoundState,
            createAnalyzerOutputWithHighIOU(result, Category.ID_FRONT)
        )

        assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
        assertThat(resultState.type).isEqualTo(ScanType.ID_FRONT)
    }

    private fun createAnalyzerOutputWithHighIOU(
        previousAnalyzerOutput: IDDetectorOutput,
        newCategory: Category? = null
    ) =
        IDDetectorOutput(
            boundingBox = BoundingBox(
                previousAnalyzerOutput.boundingBox.left + 1,
                previousAnalyzerOutput.boundingBox.top + 1,
                previousAnalyzerOutput.boundingBox.width + 1,
                previousAnalyzerOutput.boundingBox.height + 1,
            ),
            newCategory ?: previousAnalyzerOutput.category,
            previousAnalyzerOutput.resultScore,
            previousAnalyzerOutput.allScores
        )

    private fun createAnalyzerOutputWithLowIOU(previousAnalyzerOutput: IDDetectorOutput) =
        IDDetectorOutput(
            boundingBox = BoundingBox(
                previousAnalyzerOutput.boundingBox.left + 500f,
                previousAnalyzerOutput.boundingBox.top + 500f,
                previousAnalyzerOutput.boundingBox.width + 500f,
                previousAnalyzerOutput.boundingBox.height + 500f,
            ),
            previousAnalyzerOutput.category,
            previousAnalyzerOutput.resultScore,
            previousAnalyzerOutput.allScores
        )

    private companion object {
        val INITIAL_BOUNDING_BOX = BoundingBox(0f, 0f, 500f, 500f)
        val INITIAL_ID_FRONT_OUTPUT = IDDetectorOutput(
            INITIAL_BOUNDING_BOX,
            Category.ID_FRONT,
            0f,
            listOf()
        )
    }
}
