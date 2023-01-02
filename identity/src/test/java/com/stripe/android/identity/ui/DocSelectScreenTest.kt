package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.stripe.android.camera.CameraPermissionEnsureable
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentDocumentSelectPage
import com.stripe.android.identity.viewmodel.IdentityViewModel
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

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class DocSelectScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockNavController = mock<NavController>()
    private val mockCameraPermissionEnsureable = mock<CameraPermissionEnsureable>()
    private val verificationPageLiveData =
        MutableLiveData(Resource.success(mock<VerificationPage>()))

    private val mockIdentityViewModel = mock<IdentityViewModel> {
        on { verificationPage } doReturn verificationPageLiveData
    }

    private val verificationPageWithMultiChoice = mock<VerificationPage> {
        on { it.documentSelect } doReturn DOC_SELECT_MULTI_CHOICE
    }

    private val verificationPageWithSingleChoice = mock<VerificationPage> {
        on { it.documentSelect } doReturn DOC_SELECT_SINGLE_CHOICE
    }

    @Test
    fun verifyMultiChoice() {
        testDocSelectionScreen(
            Resource.success(verificationPageWithMultiChoice)
        ) {
            onNodeWithTag(docSelectionTitleTag).assertTextEquals(DOCUMENT_SELECT_TITLE)
            onNodeWithTag(singleSelectionTag).assertDoesNotExist()
            onNodeWithTag(PASSPORT_KEY).onChildAt(0)
                .assertTextEquals(PASSPORT_BUTTON_TEXT.uppercase())
            onNodeWithTag(ID_CARD_KEY).onChildAt(0)
                .assertTextEquals(ID_BUTTON_TEXT.uppercase())
            onNodeWithTag(DRIVING_LICENSE_KEY).onChildAt(0)
                .assertTextEquals(DRIVING_LICENSE_BUTTON_TEXT.uppercase())

            onNodeWithTag(DRIVING_LICENSE_KEY).onChildAt(0).performClick()
            verify(mockIdentityViewModel).postVerificationPageDataForDocSelection(
                eq(CollectedDataParam.Type.DRIVINGLICENSE),
                same(mockNavController),
                any(),
                same(mockCameraPermissionEnsureable)
            )
        }
    }

    @Test
    fun verifySingleChoice() {
        testDocSelectionScreen(
            Resource.success(verificationPageWithSingleChoice)
        ) {
            onNodeWithTag(docSelectionTitleTag).assertTextEquals(DOCUMENT_SELECT_TITLE)
            onNodeWithTag(singleSelectionTag).onChildAt(0)
                .assertTextEquals(PASSPORT_SINGLE_BODY_TEXT)
            onNodeWithTag(singleSelectionTag).onChildAt(1)
                .assertTextEquals(DOCUMENT_SELECT_BUTTON_TEXT)

            onNodeWithTag(singleSelectionTag).onChildAt(1).performClick()
            verify(mockIdentityViewModel).postVerificationPageDataForDocSelection(
                eq(CollectedDataParam.Type.PASSPORT),
                same(mockNavController),
                any(),
                same(mockCameraPermissionEnsureable)
            )
        }
    }

    private fun testDocSelectionScreen(
        verificationState: Resource<VerificationPage>,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        verificationPageLiveData.postValue(verificationState)
        composeTestRule.setContent {
            DocSelectionScreen(
                navController = mockNavController,
                identityViewModel = mockIdentityViewModel,
                cameraPermissionEnsureable = mockCameraPermissionEnsureable
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val DOCUMENT_SELECT_TITLE = "title"
        const val DOCUMENT_SELECT_BUTTON_TEXT = "button text"
        const val PASSPORT_BUTTON_TEXT = "Passport"
        const val ID_BUTTON_TEXT = "ID"
        const val DRIVING_LICENSE_BUTTON_TEXT = "Driver's license"
        const val PASSPORT_BODY_TEXT = "Passport body"
        const val PASSPORT_SINGLE_BODY_TEXT = "Passport single selection body"

        val DOC_SELECT_MULTI_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BUTTON_TEXT,
                ID_CARD_KEY to ID_BUTTON_TEXT,
                DRIVING_LICENSE_KEY to DRIVING_LICENSE_BUTTON_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = null
        )

        val DOC_SELECT_SINGLE_CHOICE = VerificationPageStaticContentDocumentSelectPage(
            title = DOCUMENT_SELECT_TITLE,
            idDocumentTypeAllowlist = mapOf(
                PASSPORT_KEY to PASSPORT_BODY_TEXT
            ),
            buttonText = DOCUMENT_SELECT_BUTTON_TEXT,
            body = PASSPORT_SINGLE_BODY_TEXT
        )
    }
}
