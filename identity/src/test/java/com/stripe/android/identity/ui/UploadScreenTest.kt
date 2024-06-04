package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.navigation.DocumentUploadDestination
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.SingleSideDocumentUploadState
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentCapturePage
import com.stripe.android.identity.utils.IdentityImageHandler
import com.stripe.android.identity.viewmodel.IdentityViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class UploadScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mock<NavController>()
    private val onPhotoSelected = mock<(UploadMethod) -> Unit>()
    private val onDismissRequest = mock<() -> Unit>()
    private val onUploadMethodSelected = mock<() -> Unit>()

    private val documentFrontUploadState = MutableStateFlow(IDLE_STATE)
    private val documentBackUploadState = MutableStateFlow(IDLE_STATE)
    private val collectedData = MutableStateFlow(CollectedDataParam())
    private val missingRequirements =
        MutableStateFlow(setOf(Requirement.IDDOCUMENTFRONT, Requirement.IDDOCUMENTBACK))

    private val cameraPermissionGranted = MutableStateFlow(true)

    private val documentCaptureRequireLiveCaptureMock =
        mock<VerificationPageStaticContentDocumentCapturePage> {
            on { requireLiveCapture } doReturn true
        }
    private val verificationPageRequireLiveCapture = mock<VerificationPage> {
        on { documentCapture } doReturn documentCaptureRequireLiveCaptureMock
    }
    private val mockImageHandler = mock<IdentityImageHandler>()

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { documentFrontUploadedState } doReturn documentFrontUploadState
        on { documentBackUploadedState } doReturn documentBackUploadState
        on { collectedData } doReturn collectedData
        on { missingRequirements } doReturn missingRequirements
        on { verificationPage } doReturn MutableLiveData(
            Resource.success(
                verificationPageRequireLiveCapture
            )
        )
        on { imageHandler } doReturn mockImageHandler
        on { cameraPermissionGranted } doReturn cameraPermissionGranted
    }

    @Test
    fun `when front is not uploaded, front upload UI is enabled and back UI is not visible`() {
        testUploadScreen {
            onNodeWithTag(FRONT_ROW_TAG).assertExists()
            onNodeWithTag(BACK_ROW_TAG).assertDoesNotExist()
            onNodeWithTag(UPLOAD_SCREEN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun `when front is uploaded and don't need back, continue button is enabled`() {
        documentFrontUploadState.update { UPLOADED_STATE }
        collectedData.update {
            it.copy(idDocumentFront = mock())
        }
        missingRequirements.update {
            setOf()
        }
        testUploadScreen {
            onNodeWithTag(FRONT_ROW_TAG).assertExists()
            onNodeWithTag(BACK_ROW_TAG).assertDoesNotExist()
            onNodeWithTag(UPLOAD_SCREEN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsEnabled()
        }
    }

    @Test
    fun `when front is uploaded and need back, back upload UI is enabled, continue button is disabled`() {
        documentFrontUploadState.update { UPLOADED_STATE }
        collectedData.update {
            it.copy(idDocumentFront = mock())
        }
        missingRequirements.update {
            setOf(Requirement.IDDOCUMENTBACK)
        }
        testUploadScreen {
            onNodeWithTag(FRONT_ROW_TAG).assertExists()
            onNodeWithTag(BACK_ROW_TAG).assertExists()
            onNodeWithTag(UPLOAD_SCREEN_CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun `when front and back are uploaded, continue button is enabled`() {
        documentFrontUploadState.update { UPLOADED_STATE }
        collectedData.update {
            it.copy(idDocumentFront = mock(), idDocumentBack = mock())
        }
        missingRequirements.update {
            setOf()
        }
        testUploadScreen {
            onNodeWithTag(FRONT_ROW_TAG).assertExists()
            onNodeWithTag(BACK_ROW_TAG).assertExists()
            onNodeWithTag(UPLOAD_SCREEN_CONTINUE_BUTTON_TAG).onChildAt(0).let { continueButton ->
                continueButton.assertIsEnabled()
                continueButton.performClick()

                runBlocking {
                    verify(mockIdentityViewModel).navigateToSelfieOrSubmit(
                        same(mockNavController),
                        eq(DocumentUploadDestination.ROUTE.route)
                    )
                }
            }
        }
    }

    // Test UploadImageDialog
    @Test
    fun `when shouldShowTakePhoto is true and shouldShowChoosePhoto is false UI is correct`() {
        testUploadImageDialog(shouldShowChoosePhoto = false) {
            onNodeWithTag(SHOULD_SHOW_TAKE_PHOTO_TAG).assertExists()
            onNodeWithTag(SHOULD_SHOW_CHOOSE_PHOTO_TAG).assertDoesNotExist()
        }
    }

    @Test
    fun `when shouldShowTakePhoto is true and shouldShowChoosePhoto is true UI is correct`() {
        testUploadImageDialog(shouldShowChoosePhoto = true) {
            onNodeWithTag(SHOULD_SHOW_TAKE_PHOTO_TAG).assertExists()
            onNodeWithTag(SHOULD_SHOW_CHOOSE_PHOTO_TAG).assertExists()
        }
    }

    @Test
    fun `when takePhoto is clicked callback is invoked`() {
        testUploadImageDialog(shouldShowChoosePhoto = true) {
            onNodeWithTag(SHOULD_SHOW_TAKE_PHOTO_TAG).assertExists()
            onNodeWithTag(SHOULD_SHOW_CHOOSE_PHOTO_TAG).assertExists()

            onNodeWithTag(SHOULD_SHOW_TAKE_PHOTO_TAG).performClick()
            verify(onUploadMethodSelected).invoke()
            verify(onPhotoSelected).invoke(eq(UploadMethod.TAKE_PHOTO))
        }
    }

    @Test
    fun `when choosePhoto is clicked callback is invoked`() {
        testUploadImageDialog(shouldShowChoosePhoto = true) {
            onNodeWithTag(SHOULD_SHOW_TAKE_PHOTO_TAG).assertExists()
            onNodeWithTag(SHOULD_SHOW_CHOOSE_PHOTO_TAG).assertExists()

            onNodeWithTag(SHOULD_SHOW_CHOOSE_PHOTO_TAG).performClick()
            verify(onUploadMethodSelected).invoke()
            verify(onPhotoSelected).invoke(eq(UploadMethod.CHOOSE_PHOTO))
        }
    }

    private fun testUploadScreen(
        testBlock: ComposeContentTestRule.() -> Unit
    ) {
        composeTestRule.setContent {
            UploadScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel
            )
        }
        with(composeTestRule, testBlock)
    }

    private fun testUploadImageDialog(
        shouldShowChoosePhoto: Boolean,
        testBlock: ComposeContentTestRule.() -> Unit
    ) {
        composeTestRule.setContent {
            UploadImageDialog(
                isFront = true,
                shouldShowTakePhoto = true,
                shouldShowChoosePhoto = shouldShowChoosePhoto,
                onPhotoSelected = onPhotoSelected,
                onDismissRequest = onDismissRequest,
                onUploadMethodSelected = onUploadMethodSelected
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        val UPLOADED_STATE = SingleSideDocumentUploadState(
            highResResult = Resource.success(mock()),
            lowResResult = Resource.success(mock())
        )

        val IDLE_STATE = SingleSideDocumentUploadState(
            highResResult = Resource.idle(),
            lowResResult = Resource.idle()
        )
    }
}
