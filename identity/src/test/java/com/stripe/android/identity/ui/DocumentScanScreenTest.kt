package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.states.IdentityScanState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class DocumentScanScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val onCameraViewCreated = mock<(CameraView) -> Unit>()
    private val onContinueClicked = mock<() -> Unit>()

    @Test
    fun verifyNullState() {
        testDocumentScanScreen() {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyInitialState() {
        testDocumentScanScreen(mock<IdentityScanState.Initial>()) {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)
            onNodeWithTag(CHECK_MARK_TAG).assertDoesNotExist()
            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    @Test
    fun verifyFinishedState() {
        testDocumentScanScreen(mock<IdentityScanState.Finished>()) {
            verify(onCameraViewCreated).invoke(any())

            onNodeWithTag(SCAN_TITLE_TAG).assertTextEquals(TITLE)
            onNodeWithTag(SCAN_MESSAGE_TAG).assertTextEquals(MESSAGE)
            onNodeWithTag(CHECK_MARK_TAG).assertExists()

            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsEnabled()
            onNodeWithTag(CONTINUE_BUTTON_TAG).assertContentDescriptionEquals().performClick()

            verify(onContinueClicked).invoke()

            onNodeWithTag(CONTINUE_BUTTON_TAG).onChildAt(0).assertIsNotEnabled()
        }
    }

    private fun testDocumentScanScreen(
        displayState: IdentityScanState? = null,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            DocumentScanScreen(
                title = TITLE,
                message = MESSAGE,
                newDisplayState = displayState,
                onCameraViewCreated = onCameraViewCreated,
                onContinueClicked = onContinueClicked
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val TITLE = "title"
        const val MESSAGE = "message"
    }
}
