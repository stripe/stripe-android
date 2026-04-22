package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieModels
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowSystemClock
import java.util.concurrent.TimeUnit
import kotlin.time.ComparableTimeMark
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
internal class FaceDetectorTransitionerTest {
    private val mockNeverTimeoutClockMark = mock<ComparableTimeMark>().also {
        whenever(it.hasPassedNow()).thenReturn(false)
    }

    private val mockAlwaysTimeoutClockMark = mock<ComparableTimeMark>().also {
        whenever(it.hasPassedNow()).thenReturn(true)
    }

    private val mockReachedStateAt = mock<ComparableTimeMark>()

    private val mockSelfieFrameSaver = mock<FaceDetectorTransitioner.SelfieFrameSaver>()

    @Test
    fun `Initial transitions to TimeOut when timeout`() = runBlocking {
        val transitioner =
            FaceDetectorTransitioner(SELFIE_CAPTURE_PAGE)
        transitioner.timeoutAt = mockAlwaysTimeoutClockMark

        assertThat(
            transitioner.transitionFromInitial(
                IdentityScanState.Initial(
                    IdentityScanState.ScanType.SELFIE,
                    transitioner
                ),
                mock(),
                mock<FaceDetectorOutput>()
            )
        ).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Initial transitions to Found when face is valid and frame is saved`() = runBlocking {
        val transitioner =
            FaceDetectorTransitioner(
                SELFIE_CAPTURE_PAGE,
                selfieFrameSaver = mockSelfieFrameSaver
            )
        transitioner.timeoutAt = mockNeverTimeoutClockMark
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.SELFIE,
            transitioner
        )

        val mockInput = mock<AnalyzerInput>()

        val resultState = transitioner.transitionFromInitial(
            initialState,
            mockInput,
            VALID_OUTPUT
        )

        verify(mockSelfieFrameSaver).saveFrame(
            argThat { input == mockInput && output == VALID_OUTPUT },
            same(VALID_OUTPUT)
        )
        assertThat(
            resultState
        ).isInstanceOf(IdentityScanState.Found::class.java)
    }

    @Test
    fun `Initial stays in Initial when face is invalid`() = runBlocking {
        val transitioner =
            FaceDetectorTransitioner(SELFIE_CAPTURE_PAGE)
        transitioner.timeoutAt = mockNeverTimeoutClockMark
        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.SELFIE,
            transitioner
        )

        val resultState = transitioner.transitionFromInitial(
            initialState,
            mock(),
            INVALID_OUTPUT
        )

        assertThat(
            resultState
        ).isInstanceOf(IdentityScanState.Initial::class.java)
    }

    @Test
    fun `Found transitions to TimeOut when timeout`() = runBlocking {
        val transitioner =
            FaceDetectorTransitioner(SELFIE_CAPTURE_PAGE)
        transitioner.timeoutAt = mockAlwaysTimeoutClockMark

        assertThat(
            transitioner.transitionFromFound(
                IdentityScanState.Found(
                    IdentityScanState.ScanType.SELFIE,
                    transitioner
                ),
                mock(),
                mock<FaceDetectorOutput>()
            )
        ).isInstanceOf(IdentityScanState.TimeOut::class.java)
    }

    @Test
    fun `Found stays in to Found when sample interval not reached`() = runBlocking {
        val transitioner =
            FaceDetectorTransitioner(SELFIE_CAPTURE_PAGE)
        transitioner.timeoutAt = mockNeverTimeoutClockMark

        whenever(mockReachedStateAt.elapsedNow()).thenReturn((SAMPLE_INTERVAL - 10).milliseconds)

        val foundState = IdentityScanState.Found(
            IdentityScanState.ScanType.SELFIE,
            transitioner,
            reachedStateAt = mockReachedStateAt
        )

        assertThat(
            transitioner.transitionFromFound(
                foundState,
                mock(),
                mock<FaceDetectorOutput>()
            )
        ).isSameInstanceAs(foundState)
    }

    @Test
    fun `Found stays in to Found when face is valid and not enough selfie collected`() =
        runBlocking {
            val transitioner =
                FaceDetectorTransitioner(
                    selfieCapturePage = SELFIE_CAPTURE_PAGE,
                    selfieFrameSaver = mockSelfieFrameSaver
                )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            whenever(mockReachedStateAt.elapsedNow()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
            whenever(mockSelfieFrameSaver.selfieCollected()).thenReturn(NUM_SAMPLES - 1)

            val foundState = IdentityScanState.Found(
                IdentityScanState.ScanType.SELFIE,
                transitioner,
                reachedStateAt = mockReachedStateAt
            )

            val resultState =
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    VALID_OUTPUT
                )

            assertThat(resultState).isNotSameInstanceAs(foundState)
            assertThat(resultState).isInstanceOf(IdentityScanState.Found::class.java)
        }

    @Test
    fun `Found transitions to Satisfed when face is valid and enough selfie collected`() =
        runBlocking {
            val transitioner =
                FaceDetectorTransitioner(
                    selfieCapturePage = SELFIE_CAPTURE_PAGE,
                    selfieFrameSaver = mockSelfieFrameSaver
                )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            whenever(mockReachedStateAt.elapsedNow()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
            whenever(mockSelfieFrameSaver.selfieCollected()).thenReturn(NUM_SAMPLES)

            assertThat(
                transitioner.transitionFromFound(
                    IdentityScanState.Found(
                        IdentityScanState.ScanType.SELFIE,
                        transitioner,
                        reachedStateAt = mockReachedStateAt
                    ),
                    mock(),
                    VALID_OUTPUT
                )
            ).isInstanceOf(IdentityScanState.Satisfied::class.java)
        }

    @Test
    fun `Found stays in Found when face is invalid`() =
        runBlocking {
            val transitioner =
                FaceDetectorTransitioner(
                    selfieCapturePage = SELFIE_CAPTURE_PAGE,
                    selfieFrameSaver = mockSelfieFrameSaver
                )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            whenever(mockReachedStateAt.elapsedNow()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
            whenever(mockSelfieFrameSaver.selfieCollected()).thenReturn(NUM_SAMPLES - 1)

            val foundState = IdentityScanState.Found(
                IdentityScanState.ScanType.SELFIE,
                transitioner,
                reachedStateAt = mockReachedStateAt
            )

            val resultState =
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    INVALID_OUTPUT
                )

            assertThat(resultState).isSameInstanceAs(foundState)
        }

    @Test
    fun `Found transitions to Unsatisfied when wait is stayInFoundDuration`() =
        runBlocking {
            val transitioner =
                FaceDetectorTransitioner(
                    selfieCapturePage = SELFIE_CAPTURE_PAGE,
                    selfieFrameSaver = mockSelfieFrameSaver,
                    stayInFoundDuration = 0 // immediately transition to Unsatisfied
                )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            whenever(mockReachedStateAt.elapsedNow()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
            whenever(mockSelfieFrameSaver.selfieCollected()).thenReturn(NUM_SAMPLES - 1)

            val foundState = IdentityScanState.Found(
                IdentityScanState.ScanType.SELFIE,
                transitioner,
                reachedStateAt = mockReachedStateAt
            )

            val resultState =
                transitioner.transitionFromFound(
                    foundState,
                    mock(),
                    INVALID_OUTPUT
                )

            assertThat(resultState).isInstanceOf(IdentityScanState.Unsatisfied::class.java)
        }

    @Test
    fun `Unsatisfied transitions to Initial`() =
        runBlocking {
            val transitioner =
                FaceDetectorTransitioner(
                    selfieCapturePage = SELFIE_CAPTURE_PAGE,
                    selfieFrameSaver = mockSelfieFrameSaver,
                )
            transitioner.timeoutAt = mockNeverTimeoutClockMark

            val transitionerSpy = spy(transitioner)

            val unsatisfiedState = IdentityScanState.Unsatisfied(
                "reason",
                IdentityScanState.ScanType.SELFIE,
                transitionerSpy
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

    @Test
    fun `filteredFrames chooses best frame based on bestFrameScore`() = runBlocking {
        val transitioner = FaceDetectorTransitioner(SELFIE_CAPTURE_PAGE)

        val oldestOutput = FaceDetectorOutput(
            boundingBox = BoundingBox(0.2f, 0.2f, 0.6f, 0.6f),
            resultScore = 0.8f
        )
        val midAOutput = FaceDetectorOutput(
            boundingBox = BoundingBox(0.21f, 0.21f, 0.6f, 0.6f),
            resultScore = 0.95f
        )
        val midBOutput = FaceDetectorOutput(
            boundingBox = BoundingBox(0.22f, 0.22f, 0.6f, 0.6f),
            resultScore = 0.7f
        )
        val newestOutput = FaceDetectorOutput(
            boundingBox = BoundingBox(0.23f, 0.23f, 0.6f, 0.6f),
            resultScore = 0.6f
        )

        // Save in chronological order; FrameSaver stores most recent at the beginning.
        transitioner.selfieFrameSaver.saveFrame(
            FaceDetectorTransitioner.SelfieFrame(mock(), oldestOutput, bestFrameScore = 0.1f),
            oldestOutput
        )
        transitioner.selfieFrameSaver.saveFrame(
            FaceDetectorTransitioner.SelfieFrame(mock(), midAOutput, bestFrameScore = 0.2f),
            midAOutput
        )
        transitioner.selfieFrameSaver.saveFrame(
            FaceDetectorTransitioner.SelfieFrame(mock(), midBOutput, bestFrameScore = 0.8f),
            midBOutput
        )
        transitioner.selfieFrameSaver.saveFrame(
            FaceDetectorTransitioner.SelfieFrame(mock(), newestOutput, bestFrameScore = 0.3f),
            newestOutput
        )

        assertThat(
            transitioner.filteredFrames[FaceDetectorTransitioner.INDEX_BEST].second
        ).isEqualTo(midBOutput)
    }

    @Test
    fun `Found does not save frame when motion blur is detected`() = runBlocking {
        val pageWithStrictIou = SELFIE_CAPTURE_PAGE.copy(
            models = SELFIE_CAPTURE_PAGE.models.copy(faceDetectorIou = 0.94f)
        )
        val transitioner = FaceDetectorTransitioner(pageWithStrictIou)
        transitioner.timeoutAt = mockNeverTimeoutClockMark

        val initialState = IdentityScanState.Initial(
            IdentityScanState.ScanType.SELFIE,
            transitioner
        )

        // First valid frame: should be saved.
        val foundState = transitioner.transitionFromInitial(
            initialState,
            mock(),
            VALID_OUTPUT
        ) as IdentityScanState.Found
        assertThat(transitioner.selfieFrameSaver.selfieCollected()).isEqualTo(1)

        // Advance time so the motion blur detector will return a non-null result.
        ShadowSystemClock.advanceBy(150, TimeUnit.MILLISECONDS)

        whenever(mockReachedStateAt.elapsedNow()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
        val foundStateWithIntervalReached = IdentityScanState.Found(
            IdentityScanState.ScanType.SELFIE,
            transitioner,
            reachedStateAt = mockReachedStateAt
        )

        val movedButStillValidOutput = FaceDetectorOutput(
            boundingBox = BoundingBox(
                left = 0.11f,
                top = 0.11f,
                width = 0.6f,
                height = 0.6f
            ),
            resultScore = VALID_SCORE
        )

        val resultState = transitioner.transitionFromFound(
            foundStateWithIntervalReached,
            mock(),
            movedButStillValidOutput
        )

        // Motion blur gating should prevent the frame from being saved.
        assertThat(resultState).isSameInstanceAs(foundStateWithIntervalReached)
        assertThat(transitioner.selfieFrameSaver.selfieCollected()).isEqualTo(1)
    }

    private companion object {

        const val SCORE_THRESHOLD = 0.8f
        const val VALID_SCORE = SCORE_THRESHOLD + 0.1f
        const val INVALID_SCORE = SCORE_THRESHOLD - 0.1f
        const val SAMPLE_INTERVAL = 200
        const val NUM_SAMPLES = 8
        val SELFIE_CAPTURE_PAGE = VerificationPageStaticContentSelfieCapturePage(
            autoCaptureTimeout = 15000,
            filePurpose = StripeFilePurpose.IdentityPrivate.code,
            numSamples = NUM_SAMPLES,
            sampleInterval = SAMPLE_INTERVAL,
            models = VerificationPageStaticContentSelfieModels(
                faceDetectorUrl = "",
                faceDetectorMinScore = SCORE_THRESHOLD,
                faceDetectorIou = 0.5f
            ),
            maxCenteredThresholdX = 0.2f,
            maxCenteredThresholdY = 0.2f,
            minEdgeThreshold = 0.05f,
            minCoverageThreshold = 0.07f,
            maxCoverageThreshold = 0.8f,
            lowResImageMaxDimension = 800,
            lowResImageCompressionQuality = 0.82f,
            highResImageMaxDimension = 1440,
            highResImageCompressionQuality = 0.92f,
            highResImageCropPadding = 0.5f,
            trainingConsentText = "consent"
        )

        val VALID_OUTPUT = FaceDetectorOutput(
            boundingBox = BoundingBox(
                0.2f,
                0.2f,
                0.6f,
                0.6f
            ),
            resultScore = VALID_SCORE
        )

        val INVALID_OUTPUT = FaceDetectorOutput(
            boundingBox = BoundingBox(
                0.2f,
                0.2f,
                0.6f,
                0.6f
            ),
            resultScore = INVALID_SCORE
        )
    }
}
