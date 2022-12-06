package com.stripe.android.identity.ui

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
import com.stripe.android.camera.CameraPreviewImage
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.ml.AnalyzerInput
import com.stripe.android.identity.ml.FaceDetectorOutput
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentSelfieCapturePage
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class SelfieScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val selfieCapturePage = mock<VerificationPageStaticContentSelfieCapturePage> {
        on { consentText } doReturn SELFIE_CONSENT_TEXT
    }
    private val verificationPage = mock<VerificationPage> {
        on { it.selfieCapture } doReturn selfieCapturePage
    }

    private val onError = mock<(Throwable) -> Unit>()
    private val onCameraViewCreated = mock<(CameraView) -> Unit>()
    private val onContinueClicked = mock<(Boolean) -> Unit>()

    private val dummyBitmap: Bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    private val mockFilteredFramePair =
        AnalyzerInput(
            CameraPreviewImage(dummyBitmap, mock()),
            mock()
        ) to mock<FaceDetectorOutput>()

    @Test
    fun verifyNullScanState() {
        testSelfieScanScreen(null) {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyInitialScanState() {
        testSelfieScanScreen(mock<IdentityScanState.Initial>()) {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyFoundScanState() {
        testSelfieScanScreen(mock<IdentityScanState.Found>()) {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifySatisfiedScanState() {
        testSelfieScanScreen(mock<IdentityScanState.Satisfied>()) {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)

            onNodeWithTag(SCAN_VIEW_TAG).assertExists()
            onNodeWithTag(RESULT_VIEW_TAG).assertDoesNotExist()

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

        val faceDetectorTransitioner = mock<FaceDetectorTransitioner>() {
            on { filteredFrames } doReturn mockFilteredFrames
        }
        val finishedState = mock<IdentityScanState.Finished> {
            on { transitioner } doReturn faceDetectorTransitioner
        }
        testSelfieScanScreen(finishedState) {
            onNodeWithTag(SELFIE_SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SELFIE_SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)

            onNodeWithTag(RESULT_VIEW_TAG).assertExists()
            onNodeWithTag(CONSENT_CHECKBOX_TAG).assertIsOff()
            onNodeWithTag(SCAN_VIEW_TAG).assertDoesNotExist()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsEnabled()

            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).performClick()
            onNodeWithTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
            verify(onContinueClicked).invoke(false)
        }
    }

    private fun testSelfieScanScreen(
        newDisplayState: IdentityScanState?,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            SelfieScanScreen(
                title = TITLE,
                message = MESSAGE,
                verificationPageState = Resource.success(verificationPage),
                onError = onError,
                newDisplayState = newDisplayState,
                onCameraViewCreated = onCameraViewCreated,
                onContinueClicked = onContinueClicked
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val SELFIE_CONSENT_TEXT = "selfie consent"
    }
}
