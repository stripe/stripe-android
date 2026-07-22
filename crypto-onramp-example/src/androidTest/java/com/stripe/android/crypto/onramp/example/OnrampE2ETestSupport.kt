package com.stripe.android.crypto.onramp.example

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.crypto.onramp.example.store.ONRAMP_PREFS_NAME
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class OnrampE2ETestRule : TestRule {
    val composeRule = createEmptyComposeRule()

    private val activityRule: ActivityScenarioRule<OnrampActivity> = activityScenarioRule()
    private val attestationFeatureFlagTestRule = FeatureFlagTestRule(
        featureFlag = FeatureFlags.nativeLinkAttestationEnabled,
        isEnabled = false
    )
    private val fixtureRule = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.getSharedPreferences(ONRAMP_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .commit()
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain.emptyRuleChain()
            .around(composeRule)
            .around(attestationFeatureFlagTestRule)
            .around(fixtureRule)
            .around(activityRule)
            .apply(base, description)
    }

    fun recreateHostActivity() {
        activityRule.scenario.recreate()
    }
}

@OptIn(ExperimentalTestApi::class)
internal class OnrampE2EPage(
    private val composeRule: ComposeTestRule,
) {
    private val defaultTimeout: Duration = 30.seconds

    fun loginAndAuthenticateWithOtp() {
        waitForTag(LOGIN_EMAIL_TAG)
        composeRule.onNodeWithTag(LOGIN_EMAIL_TAG).performTextInput(E2E_EMAIL)
        composeRule.onNodeWithTag(LOGIN_PASSWORD_TAG).performTextInput(E2E_PASSWORD)

        clickTag(LOGIN_LOGIN_BUTTON_TAG)
        clickTag(AUTHENTICATE_BUTTON_TAG)
        enterLinkOtp()
        waitForTag(AUTHENTICATED_OPERATIONS_TAG)
    }

    fun returnToSeamlessSignIn() {
        clickTag(BACK_TO_SIGN_IN_BUTTON_TAG)
        waitForTag(SEAMLESS_SIGN_IN_NOT_ME_BUTTON_TAG)
    }

    fun declineSeamlessSignIn() {
        clickTag(SEAMLESS_SIGN_IN_NOT_ME_BUTTON_TAG)
        waitForTag(LOGIN_EMAIL_TAG)
    }

    fun logOut() {
        clickTag(LOG_OUT_BUTTON_TAG)
        waitForTag(LOGIN_EMAIL_TAG)
    }

    fun registerDefaultWallet() {
        clickTag(REGISTER_WALLET_BUTTON_TAG)
        waitForSnackbar("Wallet address registered successfully!")
    }

    fun cancelCardCollection() {
        clickTag(COLLECT_CARD_BUTTON_TAG)
        waitForNode(hasContentDescription(LINK_CLOSE_DESCRIPTION))
        composeRule.onNode(hasContentDescription(LINK_CLOSE_DESCRIPTION)).performClick()
        waitForSnackbar("Payment selection cancelled")
    }

    fun collectExistingCard() {
        clickTag(COLLECT_CARD_BUTTON_TAG)

        val cvcMatcher = hasText("CVC").and(hasSetTextAction())
        if (waitForOptionalNode(cvcMatcher, timeoutMs = 5.seconds.inWholeMilliseconds)) {
            composeRule.onNode(cvcMatcher)
                .performScrollTo()
                .performTextReplacement(TEST_CARD_CVC)
        }

        clickTag(LINK_PRIMARY_BUTTON_TAG)
        waitForSelectedPayment()
    }

    fun collectBankAccount() {
        clickTag(COLLECT_BANK_ACCOUNT_BUTTON_TAG)

        if (waitForOptionalNode(hasTestTag(LINK_ADD_PAYMENT_METHOD_ROW_TAG))) {
            clickTag(LINK_ADD_PAYMENT_METHOD_ROW_TAG)
            completeFinancialConnectionsFlow()
        }

        if (waitForOptionalNode(hasTestTag(LINK_WALLET_PAY_BUTTON_TAG), timeoutMs = 10.seconds.inWholeMilliseconds)) {
            clickTag(LINK_WALLET_PAY_BUTTON_TAG, scrollRoot = false)
        }

        waitForTag(SETTLEMENT_SPEED_STANDARD_TAG, timeoutMs = 60.seconds.inWholeMilliseconds)
        composeRule.onNodeWithTag(SETTLEMENT_SPEED_STANDARD_TAG)
            .performScrollTo()
            .performClick()
        composeRule.waitForIdle()
    }

    fun createPaymentTokenAndSession() {
        createPaymentToken()
        clickTag(CREATE_SESSION_BUTTON_TAG)
        waitForTag(SESSION_STATUS_TAG, timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    fun createPaymentToken() {
        clickTag(CREATE_CRYPTO_TOKEN_BUTTON_TAG)
        waitForSnackbar("Created crypto payment token")
    }

    fun performCheckout() {
        clickTag(CHECKOUT_BUTTON_TAG, timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    fun waitForCheckoutCompleted() {
        waitForSnackbar("Checkout completed successfully!", timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    fun waitForSelectedPayment(timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        waitForTag(SELECTED_PAYMENT_TYPE_TAG, timeoutMs)
    }

    private fun enterLinkOtp() {
        waitForTag("OTP-0")
        for (index in 0..5) {
            composeRule.onNodeWithTag("OTP-$index").slowType("0")
        }
    }

    private fun completeFinancialConnectionsFlow() {
        clickTag("consent_cta", timeoutMs = 60.seconds.inWholeMilliseconds)

        if (waitForOptionalNode(hasTestTag("existing_email-button"), timeoutMs = 10.seconds.inWholeMilliseconds)) {
            clickTag("existing_email-button")
        }
        if (waitForOptionalNode(hasTestTag("test_mode_fill_button"), timeoutMs = 10.seconds.inWholeMilliseconds)) {
            clickTag("test_mode_fill_button")
        }
        if (waitForOptionalNode(hasTestTag("OTP-0"), timeoutMs = 5.seconds.inWholeMilliseconds)) {
            composeRule.onNodeWithTag("OTP-0").performTextInput("000000")
        }

        waitForTag("loaded_picker_title", timeoutMs = 60.seconds.inWholeMilliseconds)
        composeRule.onAllNodes(hasText(TEST_BANK_ACCOUNT_NAME, substring = true))
            .onFirst()
            .performScrollTo()
            .performClick()
        clickTag("link_account_picker_cta", timeoutMs = 60.seconds.inWholeMilliseconds)
        clickTag("done_button", timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    private fun waitForSnackbar(message: String, timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        composeRule.waitUntilExactlyOneExists(
            hasTestTag(SNACKBAR_TEXT_TAG).and(hasText(message, substring = true)),
            timeoutMillis = timeoutMs
        )
    }

    private fun waitForNode(matcher: SemanticsMatcher, timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        composeRule.waitUntilExactlyOneExists(matcher, timeoutMillis = timeoutMs)
    }

    fun clickTag(
        tag: String,
        timeoutMs: Long = defaultTimeout.inWholeMilliseconds,
        scrollRoot: Boolean = true,
    ) {
        waitForTag(tag, timeoutMs)
        val node = composeRule.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        if (scrollRoot) {
            scrollContentUp()
        }
        if (snackbarOverlaps(tag)) {
            composeRule.waitUntilDoesNotExist(hasTestTag(SNACKBAR_TAG), timeoutMillis = timeoutMs)
        }
        composeRule.onNodeWithTag(tag).performClick()
    }

    private fun waitForTag(tag: String, timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodes(hasTestTag(tag))
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }
        composeRule.waitUntilExactlyOneExists(hasTestTag(tag), timeoutMillis = timeoutMs)
    }

    private fun waitForOptionalNode(
        matcher: SemanticsMatcher,
        timeoutMs: Long = defaultTimeout.inWholeMilliseconds,
    ): Boolean {
        return runCatching {
            composeRule.waitUntil(timeoutMs) {
                composeRule.onAllNodes(matcher)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            true
        }.getOrElse { false }
    }

    private fun SemanticsNodeInteraction.slowType(text: String, delayMs: Long = 200) {
        text.forEach { char ->
            performTextInput(char.toString())
            Thread.sleep(delayMs)
        }
    }

    private fun scrollContentUp() {
        runCatching {
            composeRule.onRoot().performTouchInput {
                swipeUp(
                    startY = centerY + EXTRA_SCROLL_DISTANCE,
                    endY = centerY - EXTRA_SCROLL_DISTANCE,
                    durationMillis = EXTRA_SCROLL_DURATION_MILLIS
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun snackbarOverlaps(tag: String): Boolean {
        val snackbar = composeRule.onAllNodes(hasTestTag(SNACKBAR_TAG))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .firstOrNull() ?: return false
        val node = composeRule.onAllNodes(hasTestTag(tag))
            .fetchSemanticsNodes(atLeastOneRootRequired = false)
            .firstOrNull() ?: return false
        return snackbar.boundsInRoot.overlaps(node.boundsInRoot)
    }
}

internal const val E2E_EMAIL = "onramptest2@stripe.com"
internal const val E2E_PASSWORD = "testing1234"

private const val LINK_CLOSE_DESCRIPTION = "Close"
private const val LINK_PRIMARY_BUTTON_TAG = "PrimaryButtonTag"
private const val LINK_ADD_PAYMENT_METHOD_ROW_TAG = "wallet_add_payment_method_row"
private const val LINK_WALLET_PAY_BUTTON_TAG = "wallet_screen_pay_button"
private const val TEST_CARD_CVC = "321"
private const val TEST_BANK_ACCOUNT_NAME = "Success"
private const val EXTRA_SCROLL_DISTANCE = 72f
private const val EXTRA_SCROLL_DURATION_MILLIS = 50L
