package com.stripe.android.identity.states

import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.Category
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.states.IdentityScanState.ScanType
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
internal class IDDetectorTransitionerTest {
    private val mockNeverTimeoutClockMark = mock<ComparableTimeMark>().also {
        whenever(it.hasPassedNow()).thenReturn(false)
    }

    private val mockAlwaysTimeoutClockMark = mock<ComparableTimeMark>().also {
        whenever(it.hasPassedNow()).thenReturn(true)
    }

    private val mockReachedStateAt = mock<ComparableTimeMark>().also {
        whenever(it.elapsedNow()).thenReturn(0.milliseconds)
    }

    @Test
    fun `Found transitions to Found when iOUCheckPass failed`() = runBlocking {
        val transitioner =
            IDDetectorTransitioner(TIMEOUT_DURATION, blurThreshold = TEST_BLUR_THRESHOLD)
        transitioner.timeoutAt = mockNeverTimeoutClockMark

        val foundState = IdentityScanState.Found(
            ScanType.DOC_FRONT,
            transitioner,
            mockReachedStateAt,
            isFromLegacyDetector = true
        )
        // initialize previousBoundingBox
        transitioner.transitionFromFound(foundState, mock(), INITIAL_LEGACY_ID_FRONT_OUTPUT)

        // send a low IOU result
        assertThat(
            transitioner.transitionFromFound(
                foundState,
                mock(),
                createLegacyAnalyzerOutputWithLowIOU(INITIAL_LEGACY_ID_FRONT_OUTPUT)
            )
        ).isSameInstanceAs(foundState)

        // verify timer is reset
        assertThat(foundState.reachedStateAt).isNotSameInstanceAs(mockReachedStateAt)
    }

    @Test
    fun `Found transitions to Found when isBlurry`() = runBlocking {
        val transitioner =
            IDDetectorTransitioner(TIMEOUT_DURATION, blurThreshold = TEST_BLUR_THRESHOLD)
        transitioner.timeoutAt = mockNeverTimeoutClockMark

        val foundState = IdentityScanState.Found(
            ScanType.DOC_FRONT,
            transitioner,
            mockReachedStateAt,
            isFromLegacyDetector = true
        )

        // send a low IOU result with blur
        val resultState = transitioner.transitionFromFound(
            foundState,
            mock(),
            createLegacyAnalyzerOutputWithLowIOU(BLURRY_ID_FRONT_OUTPUT)
        )
        assertThat(resultState).isInstanceOf(IdentityScanState.Found::class.java)
        assertThat(resultState).isNotSameInstanceAs(foundState)
        assertThat((resultState as IdentityScanState.Found).feedbackRes)
            .isEqualTo(com.stripe.android.identity.R.string.stripe_reduce_blur_2)

        // verify timer is reset
        assertThat(foundState.reachedStateAt).isNotSameInstanceAs(mockReachedStateAt)
    }

    @Test
    fun `Legacy - Found stays in Found until best-frame window expires and transitions to Satisfied`() =
        runBlocking {
            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val foundState = IdentityScanState.Found(
                ScanType.DOC_FRONT,
                transitioner,
                mockReachedStateAt,
                isFromLegacyDetector = true
            )

            val firstGoodResult = createLegacyAnalyzerOutputWithHighIOU(INITIAL_LEGACY_ID_FRONT_OUTPUT)

            // First good frame starts the best-frame window.
            assertThat(
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    firstGoodResult
                )
            ).isSameInstanceAs(foundState)

            // Still within the 1s window.
            ShadowSystemClock.advanceBy(500, TimeUnit.MILLISECONDS)
            assertThat(
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    createLegacyAnalyzerOutputWithHighIOU(firstGoodResult)
                )
            ).isSameInstanceAs(foundState)

            // After the 1s window, transition to Satisfied even if the current frame is bad.
            ShadowSystemClock.advanceBy(600, TimeUnit.MILLISECONDS)
            val resultState = transitioner.transitionFromFound(
                foundState,
                mock(),
                createLegacyAnalyzerOutputWithLowIOU(firstGoodResult)
            )

            assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
            assertThat(resultState.type).isEqualTo(ScanType.DOC_FRONT)
        }

    @Test
    fun `Found in Modern, see a new result in Legacy, transition to Unsatisfied`() =
        runBlocking {
            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val mockModernFoundState = mock<IdentityScanState.Found>().also {
                whenever(it.type).thenReturn(ScanType.DOC_FRONT)
                whenever(it.reachedStateAt).thenReturn(mockReachedStateAt)
                whenever(it.transitioner).thenReturn(transitioner)
                whenever(it.isFromLegacyDetector).thenReturn(false)
            }

            // send a legacy result
            val resultState = transitioner.transitionFromFound(
                mockModernFoundState,
                mock(),
                mock<IDDetectorOutput.Legacy>()
            )

            assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        }

    @Test
    fun `Legacy - Found stays in Found when frames don't satisfy IoU check`() =
        runBlocking {
            val allowedUnmatchedFrames = 2
            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                allowedUnmatchedFrames = allowedUnmatchedFrames,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val foundState = IdentityScanState.Found(
                ScanType.DOC_FRONT,
                transitioner,
                mockReachedStateAt,
                isFromLegacyDetector = true
            )

            // 1st frame - a match, stays in Found
            val result = createLegacyAnalyzerOutputWithHighIOU(INITIAL_LEGACY_ID_FRONT_OUTPUT)
            assertThat(
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    result
                )
            ).isSameInstanceAs(foundState)

            // 2nd frame - a low IOU frame, stays in Found
            assertThat(
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    createLegacyAnalyzerOutputWithLowIOU(result)
                )
            ).isSameInstanceAs(foundState)

            // verify timer is reset
            assertThat(foundState.reachedStateAt).isNotSameInstanceAs(mockReachedStateAt)
        }

    @Test
    fun `Legacy - Found ignores too-many unmatched frames once a best frame exists`() =
        runBlocking {
            val allowedUnmatchedFrames = 2
            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                allowedUnmatchedFrames = allowedUnmatchedFrames,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val foundState = IdentityScanState.Found(
                ScanType.DOC_FRONT,
                transitioner,
                mockReachedStateAt,
                isFromLegacyDetector = true
            )

            // 1st frame - a match (and good), starts the best-frame window
            var result = createLegacyAnalyzerOutputWithHighIOU(INITIAL_LEGACY_ID_FRONT_OUTPUT)
            assertThat(
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    result
                )
            ).isSameInstanceAs(foundState)

            // follow up frames - mismatches beyond allowedUnmatchedFrames should not force Unsatisfied
            for (i in 1..(allowedUnmatchedFrames + 2)) {
                result = createLegacyAnalyzerOutputWithHighIOU(result, Category.ID_BACK)
                assertThat(
                    transitioner.transitionFromFound(
                        foundState,
                        mock(),
                        result
                    )
                ).isSameInstanceAs(foundState)
            }
        }

    @Test
    fun `Legacy - Found transitions to Satisfied after best-frame window even if later frames are mismatches`() =
        runBlocking {
            val allowedUnmatchedFrames = 2
            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                allowedUnmatchedFrames = allowedUnmatchedFrames,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val foundState = IdentityScanState.Found(
                ScanType.DOC_FRONT,
                transitioner,
                mockReachedStateAt,
                isFromLegacyDetector = true
            )

            // 1st frame - a match (and good), starts the best-frame window
            var result = createLegacyAnalyzerOutputWithHighIOU(INITIAL_LEGACY_ID_FRONT_OUTPUT)
            assertThat(
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    result
                )
            ).isSameInstanceAs(foundState)

            // Send enough mismatches to exceed allowedUnmatchedFrames.
            for (i in 1..(allowedUnmatchedFrames + 1)) {
                result = createLegacyAnalyzerOutputWithHighIOU(result, Category.ID_BACK)
                assertThat(
                    transitioner.transitionFromFound(
                        foundState,
                        mock(),
                        result
                    )
                ).isSameInstanceAs(foundState)
            }

            // After the 1s window, transition to Satisfied even if current frame is still a mismatch.
            ShadowSystemClock.advanceBy(1100, TimeUnit.MILLISECONDS)
            val resultState = transitioner.transitionFromFound(
                foundState,
                mock(),
                createLegacyAnalyzerOutputWithHighIOU(result, Category.ID_BACK)
            )

            assertThat(resultState).isInstanceOf(IdentityScanState.Satisfied::class.java)
            assertThat(resultState.type).isEqualTo(ScanType.DOC_FRONT)
        }

    @Test
    fun `Initial transitions to Timeout when timeout`() = runBlocking {
        val transitioner = IDDetectorTransitioner(
            timeout = TIMEOUT_DURATION,
            blurThreshold = TEST_BLUR_THRESHOLD
        )
        transitioner.timeoutAt = mockAlwaysTimeoutClockMark

        val initialState = IdentityScanState.Initial(
            ScanType.DOC_FRONT,
            transitioner
        )

        assertThat(
            transitioner.transitionFromInitial(
                initialState,
                mock(),
                createLegacyAnalyzerOutputWithLowIOU(INITIAL_LEGACY_ID_BACK_OUTPUT)
            )
        ).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Initial stays in Initial if type doesn't match`() = runBlocking {
        val transitioner = IDDetectorTransitioner(
            timeout = TIMEOUT_DURATION,
            blurThreshold = TEST_BLUR_THRESHOLD
        )
        transitioner.timeoutAt = mockNeverTimeoutClockMark

        val initialState = IdentityScanState.Initial(
            ScanType.DOC_FRONT,
            transitioner
        )

        val resultState = transitioner.transitionFromInitial(
            initialState,
            mock(),
            createLegacyAnalyzerOutputWithLowIOU(INITIAL_LEGACY_ID_BACK_OUTPUT)
        )

        assertThat(resultState).isInstanceOf(IdentityScanState.Initial::class.java)
        assertThat((resultState as IdentityScanState.Initial).type)
            .isEqualTo(ScanType.DOC_FRONT)
        assertThat(resultState.feedbackRes)
            .isEqualTo(com.stripe.android.identity.R.string.stripe_front_of_id_not_detected)
    }

    @Test
    fun `Initial transitions to Found if type does match`() = runBlocking {
        val transitioner = IDDetectorTransitioner(
            timeout = TIMEOUT_DURATION,
            blurThreshold = TEST_BLUR_THRESHOLD
        )
        transitioner.timeoutAt = mockNeverTimeoutClockMark

        val initialState = IdentityScanState.Initial(
            ScanType.DOC_FRONT,
            transitioner
        )

        assertThat(
            transitioner.transitionFromInitial(
                initialState,
                mock(),
                createLegacyAnalyzerOutputWithLowIOU(INITIAL_LEGACY_ID_FRONT_OUTPUT)
            )
        ).isInstanceOf(IdentityScanState.Found::class.java)
    }

    @Test
    fun `Satisfied transitions to Finished when displaySatisfiedDuration has passed`() =
        runBlocking {
            val mockReachAtClockMark: ComparableTimeMark = mock()
            whenever(mockReachAtClockMark.elapsedNow())
                .thenReturn((DEFAULT_DISPLAY_SATISFIED_DURATION + 1).milliseconds)

            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                displaySatisfiedDuration = DEFAULT_DISPLAY_SATISFIED_DURATION,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            assertThat(
                transitioner.transitionFromSatisfied(
                    IdentityScanState.Satisfied(
                        ScanType.DOC_FRONT,
                        transitioner,
                        reachedStateAt = mockReachAtClockMark
                    ),
                    mock(),
                    mock()
                )
            ).isInstanceOf(IdentityScanState.Finished::class.java)
        }

    @Test
    fun `Satisfied stays in Satisfied when displaySatisfiedDuration has not passed`() =
        runBlocking {
            val mockReachAtClockMark: ComparableTimeMark = mock()
            whenever(mockReachAtClockMark.elapsedNow())
                .thenReturn((DEFAULT_DISPLAY_SATISFIED_DURATION - 1).milliseconds)

            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                displaySatisfiedDuration = DEFAULT_DISPLAY_SATISFIED_DURATION,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val satisfiedState =
                IdentityScanState.Satisfied(
                    ScanType.DOC_FRONT,
                    transitioner,
                    reachedStateAt = mockReachAtClockMark
                )

            val resultState = transitioner.transitionFromSatisfied(satisfiedState, mock(), mock())

            assertThat(resultState).isSameInstanceAs(satisfiedState)
        }

    @Test
    fun `Unsatisfied transitions to Timeout when timeout`() = runBlocking {
        val transitioner = IDDetectorTransitioner(
            timeout = TIMEOUT_DURATION,
            displaySatisfiedDuration = DEFAULT_DISPLAY_SATISFIED_DURATION,
            blurThreshold = TEST_BLUR_THRESHOLD
        )
        transitioner.timeoutAt = mockAlwaysTimeoutClockMark

        assertThat(
            transitioner.transitionFromUnsatisfied(
                IdentityScanState.Unsatisfied(
                    "reason",
                    ScanType.DOC_FRONT,
                    transitioner
                ),
                mock(),
                mock()
            )
        ).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Unsatisfied stays in Unsatisfied when displaySatisfiedDuration has not passed`() =
        runBlocking {
            val mockReachAtClockMark: ComparableTimeMark = mock()
            whenever(mockReachAtClockMark.elapsedNow())
                .thenReturn((DEFAULT_DISPLAY_UNSATISFIED_DURATION - 1).milliseconds)

            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                displayUnsatisfiedDuration = DEFAULT_DISPLAY_UNSATISFIED_DURATION,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val unsatisfiedState =
                IdentityScanState.Unsatisfied(
                    "reason",
                    ScanType.DOC_FRONT,
                    transitioner,
                    reachedStateAt = mockReachAtClockMark
                )

            val resultState =
                transitioner.transitionFromUnsatisfied(
                    unsatisfiedState,
                    mock(),
                    mock()
                )

            assertThat(resultState).isSameInstanceAs(unsatisfiedState)
        }

    @Test
    fun `Unsatisfied transitions to Initial when displaySatisfiedDuration has passed`() =
        runBlocking {
            val mockReachAtClockMark: ComparableTimeMark = mock()
            whenever(mockReachAtClockMark.elapsedNow())
                .thenReturn((DEFAULT_DISPLAY_UNSATISFIED_DURATION + 1).milliseconds)

            val transitioner = IDDetectorTransitioner(
                timeout = TIMEOUT_DURATION,
                displayUnsatisfiedDuration = DEFAULT_DISPLAY_UNSATISFIED_DURATION,
                blurThreshold = TEST_BLUR_THRESHOLD
            )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val transitionerSpy = spy(transitioner)

            val unsatisfiedState =
                IdentityScanState.Unsatisfied(
                    "reason",
                    ScanType.DOC_FRONT,
                    transitionerSpy,
                    reachedStateAt = mockReachAtClockMark
                )

            val resultState =
                transitionerSpy.transitionFromUnsatisfied(
                    unsatisfiedState,
                    mock(),
                    mock()
                )

            verify(transitionerSpy).resetAndReturn()
            assertThat(resultState).isInstanceOf(IdentityScanState.Initial::class.java)
        }

    private fun createLegacyAnalyzerOutputWithHighIOU(
        previousAnalyzerOutput: IDDetectorOutput.Legacy,
        newCategory: Category? = null
    ) =
        IDDetectorOutput.Legacy(
            boundingBox = BoundingBox(
                previousAnalyzerOutput.boundingBox.left + 0.005f,
                previousAnalyzerOutput.boundingBox.top + 0.005f,
                previousAnalyzerOutput.boundingBox.width,
                previousAnalyzerOutput.boundingBox.height
            ),
            newCategory ?: previousAnalyzerOutput.category,
            previousAnalyzerOutput.resultScore,
            previousAnalyzerOutput.allScores,
            previousAnalyzerOutput.blurScore,
            DUMMYBITMAP
        )

    private fun createLegacyAnalyzerOutputWithLowIOU(previousAnalyzerOutput: IDDetectorOutput.Legacy) =
        IDDetectorOutput.Legacy(
            boundingBox = BoundingBox(
                // Shift by small amount to fail IOU but keep it centered
                previousAnalyzerOutput.boundingBox.left + 0.02f,
                previousAnalyzerOutput.boundingBox.top + 0.02f,
                previousAnalyzerOutput.boundingBox.width,
                previousAnalyzerOutput.boundingBox.height
            ),
            previousAnalyzerOutput.category,
            previousAnalyzerOutput.resultScore,
            previousAnalyzerOutput.allScores,
            previousAnalyzerOutput.blurScore,
            DUMMYBITMAP
        )

    private companion object {
        const val TEST_BLUR_THRESHOLD = 0.5f
        const val TEST_BLURRY_SCORE = 0.0f
        const val TEST_UNBLURRY_SCORE = 1.0f
        const val DEFAULT_DISPLAY_SATISFIED_DURATION = 1000
        const val DEFAULT_DISPLAY_UNSATISFIED_DURATION = 1000
        val DUMMYBITMAP = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val INITIAL_BOUNDING_BOX = BoundingBox(0.25f, 0.25f, 0.5f, 0.5f)
        val INITIAL_LEGACY_ID_FRONT_OUTPUT = IDDetectorOutput.Legacy(
            INITIAL_BOUNDING_BOX,
            Category.ID_FRONT,
            0f,
            listOf(),
            1.0f,
            DUMMYBITMAP
        )

        val INITIAL_LEGACY_ID_BACK_OUTPUT = IDDetectorOutput.Legacy(
            INITIAL_BOUNDING_BOX,
            Category.ID_BACK,
            0f,
            listOf(),
            1.0f,
            DUMMYBITMAP
        )

        val BLURRY_ID_FRONT_OUTPUT = IDDetectorOutput.Legacy(
            INITIAL_BOUNDING_BOX,
            Category.ID_FRONT,
            0f,
            listOf(),
            TEST_BLURRY_SCORE,
            DUMMYBITMAP
        )

        val TIMEOUT_DURATION = 8000.milliseconds
    }
}
