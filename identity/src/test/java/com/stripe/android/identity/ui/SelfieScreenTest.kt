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
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import androidx.test.core.app.ApplicationProvider
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.identity.R
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
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

    private val displayStateChangedFlow =
        MutableStateFlow<Pair<IdentityScanState, IdentityScanState?>?>(null)

    private val selfieCapturePage = mock<VerificationPageStaticContentSelfieCapturePage> {
        on { consentText } doReturn SELFIE_CONSENT_TEXT
    }
    private val verificationPage = mock<VerificationPage> {
        on { it.selfieCapture } doReturn selfieCapturePage
    }

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn MutableLiveData(Resource.success(verificationPage))
        on { pageAndModelFiles } doReturn mock()
        on { identityAnalyticsRequestFactory } doReturn mock()
        on { workContext } doReturn UnconfinedTestDispatcher()
        on { screenTracker } doReturn mock()
    }
    private val mockIdentityScanViewModel = mock<IdentityScanViewModel> {
        on { interimResults } doReturn mock()
        on { finalResult } doReturn mock()
        on { displayStateChangedFlow } doReturn displayStateChangedFlow
    }

    private val dummyBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val mockFilteredFramePair =
        AnalyzerInput(
            CameraPreviewImage(dummyBitmap, mock()),
            mock()
        ) to mock<FaceDetectorOutput>()

    @Test
    fun verifyNullScanState() {
        testSelfieScanScreen(null) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.position_selfie))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyInitialScanState() {
        testSelfieScanScreen(mock<IdentityScanState.Initial>()) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.position_selfie))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyFoundScanState() {
        testSelfieScanScreen(mock<IdentityScanState.Found>()) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.capturing))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifySatisfiedScanState() {
        testSelfieScanScreen(mock<IdentityScanState.Satisfied>()) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.selfie_capture_complete))

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()
            onNodeWithTag(RETAKE_SELFIE_BUTTON_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyFinishedScanState() {
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
        testSelfieScanScreen(finishedState) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(context.getString(R.string.selfie_captures))
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(context.getString(R.string.selfie_capture_complete))

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
        newDisplayState: IdentityScanState?,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        newDisplayState?.let { newDisplayState ->
            displayStateChangedFlow.update {
                newDisplayState to mock()
            }
        }
        composeTestRule.setContent {
            SelfieScanScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                identityScanViewModel = mockIdentityScanViewModel
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val SELFIE_CONSENT_TEXT = "selfie consent"
    }
}
