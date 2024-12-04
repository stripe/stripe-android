package com.stripe.android.identity.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MediatorLiveData
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.identity.R
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.camera.IdentityAggregator
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.identity.viewmodel.SelfieScanViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class SelfieScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val mockNavController = mock<NavController>()

    private val scannerStateFlow =
        MutableStateFlow<IdentityScanViewModel.State>(IdentityScanViewModel.State.Initializing)

    private val feedbackStateFlow = MutableStateFlow<Int?>(null)

    private val selfieCapturePage = mock<VerificationPageStaticContentSelfieCapturePage> {
        on { consentText } doReturn SELFIE_CONSENT_TEXT
    }
    private val verificationPage = mock<VerificationPage> {
        on { it.selfieCapture } doReturn selfieCapturePage
    }

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { pageAndModelFiles } doReturn MediatorLiveData<Resource<IdentityViewModel.PageAndModelFiles>>(
            Resource.success(
                IdentityViewModel.PageAndModelFiles(verificationPage, mock(), null)
            )
        )
        on { identityAnalyticsRequestFactory } doReturn mock()
        on { workContext } doReturn UnconfinedTestDispatcher()
        on { screenTracker } doReturn mock()
    }
    private val mockSelfieScanViewModel = mock<SelfieScanViewModel> {
        on { scannerState } doReturn scannerStateFlow
        on { fpsTracker } doReturn mock()
        on { scanFeedback } doReturn feedbackStateFlow
    }

    private val dummyBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val mockFilteredFramePair =
        AnalyzerInput(
            CameraPreviewImage(dummyBitmap, mock()),
            mock()
        ) to mock<FaceDetectorOutput>()

    @Test
    fun verifyNullScanningState() {
        testSelfieScanScreen(
            scannerState = IdentityScanViewModel.State.Scanning(),
            messageId = R.string.stripe_position_selfie
        ) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_selfie))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyInitialScanningState() {
        testSelfieScanScreen(
            scannerState = IdentityScanViewModel.State.Scanning(mock<IdentityScanState.Initial>()),
            messageId = R.string.stripe_position_selfie
        ) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_position_selfie))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyFoundScanningState() {
        testSelfieScanScreen(
            scannerState = IdentityScanViewModel.State.Scanning(mock<IdentityScanState.Found>()),
            messageId = R.string.stripe_capturing
        ) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.stripe_selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.stripe_capturing))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifySatisfiedScanningState() {
        testSelfieScanScreen(
            scannerState = IdentityScanViewModel.State.Scanning(mock<IdentityScanState.Satisfied>()),
            messageId = R.string.stripe_selfie_capture_complete
        ) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG)
                .assertTextEquals(context.getString(R.string.stripe_selfie_captures))

            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG)
                .assertTextEquals(context.getString(R.string.stripe_selfie_capture_complete))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyScannedState() {
        val mockFilteredFrames = listOf(
            mockFilteredFramePair,
            mockFilteredFramePair,
            mockFilteredFramePair
        )

        val faceDetectorTransitioner = mock<FaceDetectorTransitioner> {
            on { filteredFrames } doReturn mockFilteredFrames
        }
        val finishedState = mock<IdentityScanState.Finished> {
            on { transitioner } doReturn faceDetectorTransitioner
        }
        testSelfieScanScreen(
            scannerState = IdentityScanViewModel.State.Scanned(
                IdentityAggregator.FinalResult(
                    mock(),
                    mock<FaceDetectorOutput>(),
                    finishedState
                )
            ),
            messageId = R.string.stripe_selfie_capture_complete
        ) {
            verify(mockSelfieScanViewModel).stopScan(any())
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG)
                .assertTextEquals(context.getString(R.string.stripe_selfie_captures))

            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG)
                .assertTextEquals(context.getString(R.string.stripe_selfie_capture_complete))

            onNodeWithTag(RESULT_VIEW_TAG).assertExists()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertExists()
            onNodeWithTag(CONSENT_CHECKBOX_TAG).assertIsOff()
            onNodeWithTag(SCAN_VIEW_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsEnabled()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).performClick()
            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()

            runBlocking {
                verify(mockIdentityViewModel).collectDataForSelfieScreen(
                    same(mockNavController),
                    same(faceDetectorTransitioner),
                    eq(false)
                )
            }
        }
    }

    private fun testSelfieScanScreen(
        scannerState: IdentityScanViewModel.State,
        messageId: Int? = null,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        scannerStateFlow.update { scannerState }
        messageId?.let {
            feedbackStateFlow.value = it
        }
        composeTestRule.setContent {
            SelfieScanScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                selfieScanViewModel = mockSelfieScanViewModel
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val SELFIE_CONSENT_TEXT = "selfie consent"
    }
}
