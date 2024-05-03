package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildAt
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.identity.TestApplication
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class ErrorScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val mockTopButtonClicked: () -> Unit = mock()
    private val mockBottomButtonClicked: () -> Unit = mock()

    @Test
    fun `message1 is visible when set`() {
        setComposeTestRuleWith(
            message1 = ERROR_MESSAGE1
        ) {
            onNodeWithTag(ErrorTitleTag).assertTextEquals(ERROR_TITLE)
            onNodeWithTag(ErrorMessage1Tag).assertTextEquals(ERROR_MESSAGE1)
            onNodeWithTag(ErrorMessage2Tag).assertDoesNotExist()
            onNodeWithTag(ErrorTopButtonTag).assertDoesNotExist()
            onNodeWithTag(ErrorBottomButtonTag).assertDoesNotExist()
        }
    }

    @Test
    fun `message2 is visible when set`() {
        setComposeTestRuleWith(
            message2 = ERROR_MESSAGE2
        ) {
            onNodeWithTag(ErrorTitleTag).assertTextEquals(ERROR_TITLE)
            onNodeWithTag(ErrorMessage1Tag).assertDoesNotExist()
            onNodeWithTag(ErrorMessage2Tag).assertTextEquals(ERROR_MESSAGE2)
            onNodeWithTag(ErrorTopButtonTag).assertDoesNotExist()
            onNodeWithTag(ErrorBottomButtonTag).assertDoesNotExist()
        }
    }

    @Test
    fun `topButton is correctly set`() {
        setComposeTestRuleWith(
            topButton = ErrorScreenButton(
                buttonText = ERROR_TOP_BUTTON_TEXT,
                onButtonClick = mockTopButtonClicked
            )
        ) {
            onNodeWithTag(ErrorTitleTag).assertTextEquals(ERROR_TITLE)
            onNodeWithTag(ErrorMessage1Tag).assertDoesNotExist()
            onNodeWithTag(ErrorMessage2Tag).assertDoesNotExist()
            onNodeWithTag(ErrorTopButtonTag).onChildAt(0).assertTextEquals(ERROR_TOP_BUTTON_TEXT.uppercase())
            onNodeWithTag(ErrorBottomButtonTag).assertDoesNotExist()

            onNodeWithTag(ErrorTopButtonTag).performClick()
            verify(mockTopButtonClicked).invoke()
        }
    }

    @Test
    fun `bottomButton is correctly set`() {
        setComposeTestRuleWith(
            bottomButton = ErrorScreenButton(
                buttonText = ERROR_BOTTOM_BUTTON_TEXT,
                onButtonClick = mockBottomButtonClicked
            )
        ) {
            onNodeWithTag(ErrorTitleTag).assertTextEquals(ERROR_TITLE)
            onNodeWithTag(ErrorMessage1Tag).assertDoesNotExist()
            onNodeWithTag(ErrorMessage2Tag).assertDoesNotExist()
            onNodeWithTag(ErrorTopButtonTag).assertDoesNotExist()
            onNodeWithTag(ErrorBottomButtonTag).onChildAt(0).assertTextEquals(ERROR_BOTTOM_BUTTON_TEXT.uppercase())

            onNodeWithTag(ErrorBottomButtonTag).performClick()
            verify(mockBottomButtonClicked).invoke()
        }
    }

    @Test
    fun `all fields are correctly set`() {
        setComposeTestRuleWith(
            message1 = ERROR_MESSAGE1,
            message2 = ERROR_MESSAGE2,
            topButton = ErrorScreenButton(
                buttonText = ERROR_TOP_BUTTON_TEXT,
                onButtonClick = mockTopButtonClicked
            ),
            bottomButton = ErrorScreenButton(
                buttonText = ERROR_BOTTOM_BUTTON_TEXT,
                onButtonClick = mockBottomButtonClicked
            )
        ) {
            onNodeWithTag(ErrorTitleTag).assertTextEquals(ERROR_TITLE)
            onNodeWithTag(ErrorMessage1Tag).assertTextEquals(ERROR_MESSAGE1)
            onNodeWithTag(ErrorMessage2Tag).assertTextEquals(ERROR_MESSAGE2)
            onNodeWithTag(ErrorTopButtonTag).onChildAt(0).assertTextEquals(ERROR_TOP_BUTTON_TEXT.uppercase())
            onNodeWithTag(ErrorBottomButtonTag).onChildAt(0).assertTextEquals(ERROR_BOTTOM_BUTTON_TEXT.uppercase())

            onNodeWithTag(ErrorTopButtonTag).performClick()
            verify(mockTopButtonClicked).invoke()

            // Disabled
            onNodeWithTag(ErrorBottomButtonTag).performClick()
            verifyNoInteractions(mockBottomButtonClicked)
        }
    }

    private fun setComposeTestRuleWith(
        message1: String? = null,
        message2: String? = null,
        topButton: ErrorScreenButton? = null,
        bottomButton: ErrorScreenButton? = null,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            ErrorScreen(
                identityViewModel = mock(),
                title = ERROR_TITLE,
                message1 = message1,
                message2 = message2,
                topButton = topButton,
                bottomButton = bottomButton
            )
        }

        with(composeTestRule, testBlock)
    }

    private companion object {
        const val ERROR_TITLE = "title"
        const val ERROR_MESSAGE1 = "message1"
        const val ERROR_MESSAGE2 = "message2"
        const val ERROR_TOP_BUTTON_TEXT = "topButtonText"
        const val ERROR_BOTTOM_BUTTON_TEXT = "bottomButtonText"
    }
}
