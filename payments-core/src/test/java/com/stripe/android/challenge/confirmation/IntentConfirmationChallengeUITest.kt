package com.stripe.android.challenge.confirmation

import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
    fun `close button is displayed and invokes closeClicked`() {
        var closeClicked = false

        composeTestRule.setContent {
            IntentConfirmationChallengeUI(
                hostUrl = "http://10.0.2.2:3004",
                userAgent = "fake-user-agent",
                bridgeHandler = FakeConfirmationChallengeBridgeHandler(),
                showProgressIndicator = false,
                closeClicked = { closeClicked = true },
                webViewClientFactory = { WebViewClient() }
            )
        }

        onCloseButton().assertIsDisplayed()
        onCloseButton().performClick()

        assertThat(closeClicked).isTrue()
    }

    @Test
    fun `addBridgeHandler and loadUrl are called in correct order`() = runTest {
        val bridgeHandler = FakeConfirmationChallengeBridgeHandler()
        val webViewClient = WebViewClient()
        var fakeWebView: FakeIntentConfirmationChallengeWebView? = null

        composeTestRule.setContent {
            IntentConfirmationChallengeUI(
                hostUrl = "http://10.0.2.2:3004",
                userAgent = "fake-user-agent",
                bridgeHandler = bridgeHandler,
                showProgressIndicator = false,
                closeClicked = {},
                webViewFactory = { context ->
                    FakeIntentConfirmationChallengeWebView(context)
                        .also { fakeWebView = it }
                },
                webViewClientFactory = { webViewClient }
            )
        }

        assertThat(fakeWebView?.awaitCall())
            .isEqualTo(FakeIntentConfirmationChallengeWebView.Call.SetWebViewClient(webViewClient))
        assertThat(fakeWebView?.awaitCall())
            .isEqualTo(FakeIntentConfirmationChallengeWebView.Call.AddBridgeHandler(bridgeHandler))
        assertThat(fakeWebView?.awaitCall())
            .isEqualTo(FakeIntentConfirmationChallengeWebView.Call.UpdateUserAgent("fake-user-agent"))
        assertThat(fakeWebView?.awaitCall())
            .isEqualTo(FakeIntentConfirmationChallengeWebView.Call.LoadUrl("http://10.0.2.2:3004"))
        fakeWebView?.ensureAllEventsConsumed()
    }

    private fun setContent(showProgressIndicator: Boolean) = composeTestRule.setContent {
        IntentConfirmationChallengeUI(
            hostUrl = "http://10.0.2.2:3004",
            userAgent = "fake-user-agent",
            bridgeHandler = FakeConfirmationChallengeBridgeHandler(),
            showProgressIndicator = showProgressIndicator,
            closeClicked = {},
            webViewClientFactory = { WebViewClient() }
        )
    }

    private fun onLoader() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_LOADER_TAG)
    private fun onWebView() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_WEB_VIEW_TAG)
    private fun onCloseButton() = composeTestRule.onNodeWithTag(INTENT_CONFIRMATION_CHALLENGE_CLOSE_BUTTON_TAG)
}
