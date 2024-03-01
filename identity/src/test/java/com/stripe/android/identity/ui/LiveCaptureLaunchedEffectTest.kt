package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.analytics.AnalyticsState
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class LiveCaptureLaunchedEffectTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockDocumentCapture = mock<VerificationPageStaticContentDocumentCapturePage> {
        on { requireLiveCapture } doReturn true
    }
    private val mockVerificationPage = mock<VerificationPage> {
        on { documentCapture } doReturn mockDocumentCapture
    }
    private val pageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModelFiles } doReturn pageAndModel
        on { workContext } doReturn UnconfinedTestDispatcher()
    }
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel>()
    private val mockNavController = mock<NavController>()

    @Test
    fun verifyFaceDetectorFinishedResult() {
        val faceScannedState = IdentityScanViewModel.State.Scanned(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = FaceDetectorOutput(
                    boundingBox = mock(),
                    resultScore = FACE_SCORE
                ),
                identityState = IdentityScanState.Finished(
                    type = IdentityScanState.ScanType.SELFIE,
                    transitioner = mock()
                )
            )
        )

        testLiveCaptureLaunchedEffect(
            scannerState = faceScannedState
        ) {
            verify(mockIdentityViewModel).updateAnalyticsState(
                argWhere { block ->
                    block(AnalyticsState()).selfieModelScore == FACE_SCORE
                }
            )
        }
    }

    @Test
    fun verifyFaceDetectorTimeOutResult() {
        val faceTimeoutState = IdentityScanViewModel.State.Timeout(fromSelfie = true)

        testLiveCaptureLaunchedEffect(
            scannerState = faceTimeoutState
        ) {
            verify(mockNavController).navigate(
                eq(
                    "${CouldNotCaptureDestination.COULD_NOT_CAPTURE}?${CouldNotCaptureDestination.ARG_FROM_SELFIE}=true"
                ),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyIDDetectorFrontFinishedResult() {
        val idScannedState = IdentityScanViewModel.State.Scanned(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = IDDetectorOutput.Legacy(
                    boundingBox = mock(),
                    category = mock(),
                    resultScore = ID_FRONT_MODEL_SCORE,
                    allScores = mock(),
                    blurScore = ID_FRONT_BLUR_SCORE
                ),
                identityState = IdentityScanState.Finished(
                    type = IdentityScanState.ScanType.DOC_FRONT,
                    transitioner = mock()
                )
            )
        )

        testLiveCaptureLaunchedEffect(
            scannerState = idScannedState
        ) {
            verify(mockIdentityViewModel).updateAnalyticsState(
                argWhere { block ->
                    val state = block(AnalyticsState())
                    state.docFrontModelScore == ID_FRONT_MODEL_SCORE &&
                        state.docFrontBlurScore == ID_FRONT_BLUR_SCORE
                }
            )
        }
    }

    @Test
    fun verifyIDDetectorFrontTimeOutResult() {
        val idTimeoutState = IdentityScanViewModel.State.Timeout(fromSelfie = false)

        testLiveCaptureLaunchedEffect(
            scannerState = idTimeoutState
        ) {
            verify(mockNavController).navigate(
                eq(
                    "${CouldNotCaptureDestination.COULD_NOT_CAPTURE}?" +
                        "${CouldNotCaptureDestination.ARG_FROM_SELFIE}=false"
                ),
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyIDDetectorBackFinishedResult() {
        val idScannedState = IdentityScanViewModel.State.Scanned(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = IDDetectorOutput.Legacy(
                    boundingBox = mock(),
                    category = mock(),
                    resultScore = ID_BACK_MODEL_SCORE,
                    allScores = mock(),
                    blurScore = ID_BACK_BLUR_SCORE
                ),
                identityState = IdentityScanState.Finished(
                    type = IdentityScanState.ScanType.DOC_BACK,
                    transitioner = mock()
                )
            )
        )

        testLiveCaptureLaunchedEffect(
            scannerState = idScannedState
        ) {
            verify(mockIdentityViewModel).updateAnalyticsState(
                argWhere { block ->
                    val state = block(AnalyticsState())
                    state.docBackModelScore == ID_BACK_MODEL_SCORE &&
                        state.docBackBlurScore == ID_BACK_BLUR_SCORE
                }
            )
        }
    }

    private fun testLiveCaptureLaunchedEffect(
        scannerState: IdentityScanViewModel.State,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            LiveCaptureLaunchedEffect(
                scannerState = scannerState,
                identityScanViewModel = mockIdentityScanViewModel,
                identityViewModel = mockIdentityViewModel,
                lifecycleOwner = LocalLifecycleOwner.current,
                verificationPage = mockVerificationPage,
                navController = mockNavController
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val FACE_SCORE = 123f
        const val ID_FRONT_MODEL_SCORE = 456f
        const val ID_BACK_MODEL_SCORE = 789f
        const val ID_FRONT_BLUR_SCORE = 0.1f
        const val ID_BACK_BLUR_SCORE = 0.2f
    }
}
