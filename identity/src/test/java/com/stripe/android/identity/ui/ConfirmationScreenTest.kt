package com.stripe.android.identity.ui

import android.os.Build
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.stripe.android.identity.TestApplication
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.models.VerificationPage
import com.stripe.android.identity.networking.models.VerificationPageStaticContentTextPage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [Build.VERSION_CODES.Q])
class ConfirmationScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val onConfirmedMock = mock<() -> Unit>()

    private val verificationPage = mock<VerificationPage>().also {
        whenever(it.success).thenReturn(
            VerificationPageStaticContentTextPage(
                body = CONFIRMATION_BODY,
                buttonText = CONFIRMATION_BUTTON_TEXT,
                title = CONFIRMATION_TITLE
            )
        )
    }

    @Test
    fun verifyUIIsBoundAndButtonInteracts() {
        testConfirmationScreen(
            Resource.success(verificationPage)
        ) {
            onNodeWithTag(confirmationTitleTag).assertTextEquals(CONFIRMATION_TITLE)
            onNodeWithTag(confirmationConfirmButtonTag).assertTextEquals(CONFIRMATION_BUTTON_TEXT)
            onNodeWithTag(confirmationConfirmButtonTag).performClick()
            verify(onConfirmedMock).invoke()
        }
    }

    private fun testConfirmationScreen(
        verificationState: Resource<VerificationPage>,
        testBlock: ComposeContentTestRule.() -> Unit = {}
    ) {
        composeTestRule.setContent {
            ConfirmationScreen(
                verificationPageState = verificationState,
                onConfirmed = onConfirmedMock
            )
        }
        with(composeTestRule, testBlock)
    }

    private companion object {
        const val CONFIRMATION_TITLE = "title"
        const val CONFIRMATION_BODY = "this is the confirmation body"
        const val CONFIRMATION_BUTTON_TEXT = "confirmation"
    }
}
