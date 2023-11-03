package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.analytics.AnalyticsState
import com.stripe.android.identity.analytics.FPSTracker
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory
import com.stripe.android.identity.analytics.ScreenTracker
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.ml.IDDetectorOutput
import com.stripe.android.identity.navigation.CouldNotCaptureDestination.Companion.COULD_NOT_CAPTURE
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.SingleLiveEvent
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class CameraScreenLaunchedEffectTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockDocumentCapture = mock<VerificationPageStaticContentDocumentCapturePage> {
        on { requireLiveCapture } doReturn true
    }
    private val mockVerificationPage = mock<VerificationPage> {
        on { documentCapture } doReturn mockDocumentCapture
    }
    private val mockFpsTracker = mock<FPSTracker>()
    private val mockScreenTracker = mock<ScreenTracker>()
    private val pageAndModel = MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>()
    private val mockIdentityAnalyticsRequestFactory = mock<IdentityAnalyticsRequestFactory>()
    private val interimResults = MutableLiveData<IdentityAggregator.InterimResult>()
    private val finalResult = SingleLiveEvent<IdentityAggregator.FinalResult>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { fpsTracker } doReturn mockFpsTracker
        on { pageAndModelFiles } doReturn pageAndModel
        on { identityAnalyticsRequestFactory } doReturn mockIdentityAnalyticsRequestFactory
        on { workContext } doReturn UnconfinedTestDispatcher()
        on { screenTracker } doReturn mockScreenTracker
    }
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { interimResults } doReturn interimResults
        on { finalResult } doReturn finalResult
    }

    private val mockNavController = mock<NavController>()

    @Test
    fun `when interimResults is available, fps is tracked`() {
        interimResults.postValue(mock())

        testCameraScreenLaunchedEffect {
            verify(mockFpsTracker).trackFrame()
        }
    }

    @Test
    fun verifyFaceDetectorFinishedResult() {
        finalResult.postValue(
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

        testCameraScreenLaunchedEffect {
            runBlocking {
                verify(mockFpsTracker).reportAndReset(
                    eq(IdentityAnalyticsRequestFactory.TYPE_SELFIE)
                )
            }

            verify(mockIdentityViewModel).updateAnalyticsState(
                argWhere { block ->
                    block(AnalyticsState()).selfieModelScore == FACE_SCORE
                }
            )
        }
    }

    @Test
    fun verifyFaceDetectorTimeOutResult() {
        finalResult.postValue(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = FaceDetectorOutput(
                    boundingBox = mock(),
                    resultScore = FACE_SCORE
                ),
                identityState = IdentityScanState.TimeOut(
                    type = IdentityScanState.ScanType.SELFIE,
                    transitioner = mock()
                )
            )
        )

        testCameraScreenLaunchedEffect {
            runBlocking {
                verify(mockFpsTracker).reportAndReset(
                    eq(IdentityAnalyticsRequestFactory.TYPE_SELFIE)
                )
            }

            verify(mockIdentityAnalyticsRequestFactory).selfieTimeout()

            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(COULD_NOT_CAPTURE)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyIDDetectorFrontFinishedResult() {
        finalResult.postValue(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = IDDetectorOutput(
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

        testCameraScreenLaunchedEffect {
            runBlocking {
                verify(mockFpsTracker).reportAndReset(
                    eq(IdentityAnalyticsRequestFactory.TYPE_DOCUMENT)
                )
            }

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
        finalResult.postValue(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = IDDetectorOutput(
                    boundingBox = mock(),
                    category = mock(),
                    resultScore = ID_FRONT_MODEL_SCORE,
                    allScores = mock(),
                    blurScore = ID_FRONT_BLUR_SCORE
                ),
                identityState = IdentityScanState.TimeOut(
                    type = IdentityScanState.ScanType.DOC_FRONT,
                    transitioner = mock()
                )
            )
        )

        testCameraScreenLaunchedEffect {
            runBlocking {
                verify(mockFpsTracker).reportAndReset(
                    eq(IdentityAnalyticsRequestFactory.TYPE_DOCUMENT)
                )
            }

            verify(mockIdentityAnalyticsRequestFactory).documentTimeout(
                eq(IdentityScanState.ScanType.DOC_FRONT)
            )
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(COULD_NOT_CAPTURE)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    @Test
    fun verifyIDDetectorBackFinishedResult() {
        finalResult.postValue(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = IDDetectorOutput(
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

        testCameraScreenLaunchedEffect {
            runBlocking {
                verify(mockFpsTracker).reportAndReset(
                    eq(IdentityAnalyticsRequestFactory.TYPE_DOCUMENT)
                )
            }

            verify(mockIdentityViewModel).updateAnalyticsState(
                argWhere { block ->
                    val state = block(AnalyticsState())
                    state.docBackModelScore == ID_BACK_MODEL_SCORE &&
                        state.docBackBlurScore == ID_BACK_BLUR_SCORE
                }
            )
        }
    }

    @Test
    fun verifyIDDetectorBackTimeOutResult() {
        finalResult.postValue(
            IdentityAggregator.FinalResult(
                frame = mock(),
                result = IDDetectorOutput(
                    boundingBox = mock(),
                    category = mock(),
                    resultScore = ID_BACK_MODEL_SCORE,
                    allScores = mock(),
                    blurScore = 1.0f
                ),
                identityState = IdentityScanState.TimeOut(
                    type = IdentityScanState.ScanType.DOC_BACK,
                    transitioner = mock()
                )
            )
        )

        testCameraScreenLaunchedEffect {
            runBlocking {
                verify(mockFpsTracker).reportAndReset(
                    eq(IdentityAnalyticsRequestFactory.TYPE_DOCUMENT)
                )
            }

            verify(mockIdentityAnalyticsRequestFactory).documentTimeout(
                eq(IdentityScanState.ScanType.DOC_BACK)
            )
            verify(mockNavController).navigate(
                argWhere {
                    it.startsWith(COULD_NOT_CAPTURE)
                },
                any<NavOptionsBuilder.() -> Unit>()
            )
        }
    }

    private fun testCameraScreenLaunchedEffect(
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            CameraScreenLaunchedEffect(
                identityViewModel = mockIdentityViewModel,
                identityScanViewModel = mockIdentityScanViewModel,
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
