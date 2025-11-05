package com.stripe.android.challenge.confirmation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class IntentConfirmationChallengeUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `when showWebView is false then loader is displayed`() {
        setContent(showWebView = false)

        onLoader().assertExists()
        onLoader().assertIsDisplayed()
    }

    @Test
    fun `when showWebView is true then webview is displayed`() {
        setContent(showWebView = true)

        onWebView().assertExists()
    }

    @Test
    fun `when showWebView is true then loader is not displayed`() {
        setContent(showWebView = true)

        onLoader().assertDoesNotExist()
    }

    private fun setContent(showWebView: Boolean) = composeTestRule.setContent {
        IntentConfirmationChallengeUI(
            bridgeHandler = FakeConfirmationChallengeBridgeHandler(),
            showWebView = showWebView
        )
    }

    private fun onLoader() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG)
    private fun onWebView() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG)
}
