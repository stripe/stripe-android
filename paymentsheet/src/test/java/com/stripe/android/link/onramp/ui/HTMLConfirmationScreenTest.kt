package com.stripe.android.link.onramp.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.stripe.android.link.ui.PrimaryButtonTag
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.testing.createComposeCleanupRule
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class HTMLConfirmationScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @get:Rule
    val coroutineTestRule = CoroutineTestRule(UnconfinedTestDispatcher())

    @Test
    fun `actions are enabled before confirmation starts`() = runScenario(
        isProcessing = false,
    ) {
        composeRule.onNodeWithTag(PrimaryButtonTag).assertIsEnabled()
        composeRule.onNodeWithTag(HTML_CONFIRMATION_CANCEL_BUTTON_TAG).assertIsEnabled()
    }

    @Test
    fun `actions are disabled while confirmation is processing`() = runScenario(
        isProcessing = true,
    ) {
        composeRule.onNodeWithTag(PrimaryButtonTag).assertIsNotEnabled()
        composeRule.onNodeWithTag(HTML_CONFIRMATION_CANCEL_BUTTON_TAG).assertIsNotEnabled()
    }

    private fun runScenario(
        isProcessing: Boolean,
        block: () -> Unit,
    ) {
        composeRule.setContent {
            HTMLConfirmationScreen(
                appearance = null,
                heading = "Terms and Conditions",
                html = "<p>Please accept these terms.</p>",
                confirmationButtonText = "Accept",
                cancelButtonText = "Cancel",
                isProcessing = isProcessing,
                onClose = {},
                onConfirm = {},
            )
        }
        block()
    }
}
