package com.stripe.android.identity.states

import com.google.common.truth.Truth.assertThat
import com.stripe.android.camera.framework.time.ClockMark
import com.stripe.android.camera.framework.time.milliseconds
import com.stripe.android.core.model.StripeFilePurpose
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.BoundingBox
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieModels
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class FaceDetectorTransitionerTest {
    private val mockNeverTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(false)
    }

    private val mockAlwaysTimeoutClockMark = mock<ClockMark>().also {
        whenever(it.hasPassed()).thenReturn(true)
    }

    private val mockReachedStateAt = mock<ClockMark>()

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
            eq((mockInput to VALID_OUTPUT)),
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

        whenever(mockReachedStateAt.elapsedSince()).thenReturn((SAMPLE_INTERVAL - 10).milliseconds)

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

            whenever(mockReachedStateAt.elapsedSince()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
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

            whenever(mockReachedStateAt.elapsedSince()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
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

            whenever(mockReachedStateAt.elapsedSince()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
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

            whenever(mockReachedStateAt.elapsedSince()).thenReturn((SAMPLE_INTERVAL + 10).milliseconds)
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
            consentText = "consent"
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
