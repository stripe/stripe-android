package com.stripe.android.crypto.onramp.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
        composeRule.waitUntilExactlyOneExists(
            hasTestTag(LOGIN_EMAIL_TAG),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(LOGIN_EMAIL_TAG)
            .performTextInput("twig@lickability.net")

        composeRule.onNodeWithTag(LOGIN_PASSWORD_TAG)
            .performTextInput("test1234")

        composeRule.onNodeWithTag(LOGIN_LOGIN_BUTTON_TAG)
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(AUTHENTICATE_BUTTON_TAG),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(AUTHENTICATE_BUTTON_TAG)
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            matcher = hasTestTag("OTP-0"),
            timeoutMillis = defaultTimeout.inWholeMilliseconds,
        )

        for (i in 0..5) {
            composeRule
                .onNodeWithTag("OTP-$i")
                .slowType("0")
        }

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(REGISTER_WALLET_BUTTON_TAG),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(REGISTER_WALLET_BUTTON_TAG)
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(COLLECT_CARD_BUTTON_TAG),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(COLLECT_CARD_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag("PrimaryButtonTag"),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag("PrimaryButtonTag")
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(CREATE_CRYPTO_TOKEN_BUTTON_TAG),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(CREATE_CRYPTO_TOKEN_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(CREATE_SESSION_BUTTON_TAG),
            timeoutMillis = defaultTimeout.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(CREATE_SESSION_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(CHECKOUT_BUTTON_TAG),
            timeoutMillis = 30.seconds.inWholeMilliseconds
        )

        composeRule.onNodeWithTag(CHECKOUT_BUTTON_TAG)
            .performScrollTo()
            .performClick()

        composeRule.waitUntilExactlyOneExists(
            hasTestTag(AUTHENTICATE_BUTTON_TAG),
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
}