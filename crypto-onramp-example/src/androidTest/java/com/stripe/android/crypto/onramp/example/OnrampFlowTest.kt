package com.stripe.android.crypto.onramp.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class OnrampFlowTest {
    @get:Rule
    internal val composeRule = createAndroidComposeRule<OnrampActivity>()

    private val defaultTimeout: Duration = 15.seconds

    @Before
    fun clearPrefs() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        context.getSharedPreferences(ONRAMP_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testCheckoutFlow() {
        waitForTag(LOGIN_EMAIL_TAG)

        // Enter test login credentials previously registered with the demo backend.
        composeRule.onNodeWithTag(LOGIN_EMAIL_TAG)
            .performTextInput("onramptest@stripe.com")

        composeRule.onNodeWithTag(LOGIN_PASSWORD_TAG)
            .performTextInput("testing1234")

        performClickOnNode(LOGIN_LOGIN_BUTTON_TAG)

        val snackbarText = getSnackbarText()
        if (snackbarText != null) {
            println("Snackbar text: $snackbarText")
            throw AssertionError("Snackbar shown: $snackbarText")
        } else {
            throw AssertionError("Snackbar never shown")
        }

        performClickOnNode(AUTHENTICATE_BUTTON_TAG)

        waitForTag("OTP-0")

        for (i in 0..5) {
            composeRule
                .onNodeWithTag("OTP-$i")
                .slowType("0")
        }

        performClickOnNode(REGISTER_WALLET_BUTTON_TAG)
        performClickOnNode(COLLECT_CARD_BUTTON_TAG)

        // Optionally wait for a CVV field and input if present
        val cvvMatcher = hasSetTextAction()
        if (waitForOptionalNode(cvvMatcher, timeoutMs = 5.seconds.inWholeMilliseconds)) {
            val cvvNode = try {
                composeRule.onNode(cvvMatcher)
            } catch (_: Throwable) {
                composeRule.onNode(cvvMatcher, useUnmergedTree = true)
            }
            cvvNode.performScrollTo()
            cvvNode.performTextInput("555")
        }

        performClickOnNode("PrimaryButtonTag")
        performClickOnNode(CREATE_CRYPTO_TOKEN_BUTTON_TAG)
        performClickOnNode(CREATE_SESSION_BUTTON_TAG)
        performClickOnNode(CHECKOUT_BUTTON_TAG, timeoutMs = 30.seconds.inWholeMilliseconds)

        composeRule.waitUntilExactlyOneExists(
            hasText("Checkout completed successfully!"),
            timeoutMillis = 30.seconds.inWholeMilliseconds
        )
    }

    private fun SemanticsNodeInteraction.slowType(
        text: String,
        delayMs: Long = 200
    ) {
        text.forEach { char ->
            this.performTextInput(char.toString())
            Thread.sleep(delayMs)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    private fun waitForSnackbarToHide(timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        composeRule.waitUntilDoesNotExist(hasTestTag(SNACKBAR_TAG), timeoutMillis = timeoutMs)
    }

    private fun waitForTag(tag: String, timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        composeRule.waitUntilExactlyOneExists(
            hasTestTag(tag),
            timeoutMillis = timeoutMs
        )
    }

    private fun waitForOptionalNode(
        matcher: SemanticsMatcher,
        timeoutMs: Long = defaultTimeout.inWholeMilliseconds
    ): Boolean {
        return runCatching {
            composeRule.waitUntil(timeoutMs) {
                composeRule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()
            }
            true
        }.getOrElse { false }
    }

    private fun performClickOnNode(tag: String, timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        waitForTag(tag = tag, timeoutMs = timeoutMs)
        waitForSnackbarToHide()

        val node = composeRule.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
    }

    @OptIn(ExperimentalTestApi::class)
    private fun getSnackbarText(timeoutMs: Long = defaultTimeout.inWholeMilliseconds): String? {
        runCatching {
            composeRule.waitUntil(timeoutMs) {
                val merged = composeRule.onAllNodes(hasTestTag(SNACKBAR_TAG))
                    .fetchSemanticsNodes().isNotEmpty()
                val unmerged = composeRule.onAllNodes(hasTestTag(SNACKBAR_TAG), useUnmergedTree = true)
                    .fetchSemanticsNodes().isNotEmpty()
                merged || unmerged
            }
        }

        runCatching {
            val nodes = composeRule.onAllNodes(hasTestTag(SNACKBAR_TAG))
                .fetchSemanticsNodes()
            nodes.firstOrNull()?.let { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            }?.let { return it }
        }

        return runCatching {
            val nodes = composeRule.onAllNodes(hasTestTag(SNACKBAR_TAG), useUnmergedTree = true)
                .fetchSemanticsNodes()
            nodes.firstOrNull()?.let { node ->
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }
            }
        }.getOrNull()
    }
}
