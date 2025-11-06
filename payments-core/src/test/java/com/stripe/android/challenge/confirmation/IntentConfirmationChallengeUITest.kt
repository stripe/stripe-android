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
    fun `when bridgeReady is false then loader is displayed`() {
        setContent(bridgeReady = false)

        onLoader().assertExists()
        onWebView().assertIsDisplayed()
    }

    @Test
    fun `when bridgeReady emits true then loader is not displayed`() {
        setContent(bridgeReady = true)

        onLoader().assertDoesNotExist()
        onWebView().assertIsDisplayed()
    }

    private fun setContent(bridgeReady: Boolean) = composeTestRule.setContent {
        IntentConfirmationChallengeUI(
            bridgeHandler = FakeConfirmationChallengeBridgeHandler(),
            bridgeReady = bridgeReady
        )
    }

    private fun onLoader() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG)
    private fun onWebView() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG)
}
