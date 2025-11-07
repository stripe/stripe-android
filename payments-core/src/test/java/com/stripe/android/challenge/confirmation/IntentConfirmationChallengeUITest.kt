package com.stripe.android.challenge.confirmation

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class IntentConfirmationChallengeUITest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `when showProgressIndicator is true then loader is displayed`() {
        setContent(showProgressIndicator = true)

        onLoader().assertExists()
        onWebView().assertIsDisplayed()
    }

    @Test
    fun `when showProgressIndicator is false then loader is not displayed`() {
        setContent(showProgressIndicator = false)

        onLoader().assertDoesNotExist()
        onWebView().assertIsDisplayed()
    }

    @Test
    fun `addBridgeHandler and loadUrl are called in correct order`() = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()
        var fakeWebView: FakeIntentConfirmationChallengeWebView? = null

        composeTestRule.setContent {
            IntentConfirmationChallengeUI(
                bridgeHandler = bridgeHandler,
                showProgressIndicator = false,
                webViewFactory = { context ->
                    FakeIntentConfirmationChallengeWebView(context)
                        .also { fakeWebView = it }
                }
            )
        }

        assertThat(fakeWebView?.awaitCall())
            .isEqualTo(FakeIntentConfirmationChallengeWebView.Call.AddBridgeHandler(bridgeHandler))
        assertThat(fakeWebView?.awaitCall())
            .isEqualTo(FakeIntentConfirmationChallengeWebView.Call.LoadUrl("http://10.0.2.2:3004"))
        fakeWebView?.ensureAllEventsConsumed()
    }

    private fun setContent(showProgressIndicator: Boolean) = composeTestRule.setContent {
        IntentConfirmationChallengeUI(
            bridgeHandler = FakeConfirmationChallengeBridgeHandler(),
            showProgressIndicator = showProgressIndicator
        )
    }

    private fun onLoader() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG)
    private fun onWebView() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG)
}
