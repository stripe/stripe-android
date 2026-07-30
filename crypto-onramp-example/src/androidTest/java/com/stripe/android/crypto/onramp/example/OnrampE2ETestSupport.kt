package com.stripe.android.crypto.onramp.example

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.crypto.onramp.example.store.ONRAMP_PREFS_NAME
import com.stripe.android.testing.FeatureFlagTestRule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.UUID
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
    private val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun loginAndAuthenticateWithOtp() {
        waitForTag(LOGIN_EMAIL_TAG)
        composeRule.onNodeWithTag(LOGIN_EMAIL_TAG).performTextInput(E2E_EMAIL)
        composeRule.onNodeWithTag(LOGIN_PASSWORD_TAG).performTextInput(E2E_PASSWORD)

        clickTag(LOGIN_LOGIN_BUTTON_TAG)
        clickTag(AUTHENTICATE_BUTTON_TAG)
        enterLinkOtp()
        waitForTag(AUTHENTICATED_OPERATIONS_TAG)
    }

    fun registerAndAuthenticateFreshUser(country: String): FreshOnrampUser {
        val user = FreshOnrampUser.create(country)

        waitForTag(LOGIN_EMAIL_TAG)
        replaceTag(LOGIN_EMAIL_TAG, user.email)
        replaceTag(LOGIN_PASSWORD_TAG, user.password)
        hideKeyboard()
        clickTag(LOGIN_REGISTER_BUTTON_TAG)

        replaceTag(REGISTRATION_PHONE_TAG, user.phone)
        replaceTag(REGISTRATION_COUNTRY_TAG, user.country)
        replaceTag(REGISTRATION_FULL_NAME_TAG, user.name)
        assertEditableText(REGISTRATION_PHONE_TAG, user.phone)
        assertEditableText(REGISTRATION_COUNTRY_TAG, user.country)
        assertEditableText(REGISTRATION_FULL_NAME_TAG, user.name)
        hideKeyboard()
        clickTag(REGISTRATION_REGISTER_BUTTON_TAG)

        clickTag(AUTHENTICATE_BUTTON_TAG)
        enterLinkOtp()
        acceptOAuthConsentIfShown()
        waitForTag(AUTHENTICATED_OPERATIONS_TAG, timeoutMs = 60.seconds.inWholeMilliseconds)

        return user
    }

    fun collectKycInfo(user: FreshOnrampUser) {
        val address = TestKycAddress.forCountry(user.country)

        clickTag(KYC_SECTION_TAG)
        replaceTag(KYC_FIRST_NAME_TAG, TEST_KYC_FIRST_NAME)
        replaceTag(KYC_LAST_NAME_TAG, user.name)
        replaceTag(KYC_BIRTH_COUNTRY_TAG, user.country)
        replaceTag(KYC_BIRTH_CITY_TAG, address.city)
        replaceTag(KYC_NATIONALITIES_TAG, user.country)
        replaceTag(KYC_ADDRESS_LINE_1_TAG, TEST_KYC_ADDRESS_LINE_1)
        replaceTag(KYC_ADDRESS_CITY_TAG, address.city)
        replaceTag(KYC_ADDRESS_STATE_TAG, address.state)
        replaceTag(KYC_ADDRESS_COUNTRY_TAG, address.country)
        replaceTag(KYC_ADDRESS_POSTAL_CODE_TAG, address.postalCode)
        hideKeyboard()

        clickTag(COLLECT_KYC_BUTTON_TAG, timeoutMs = 60.seconds.inWholeMilliseconds)
        waitForSnackbar("KYC Collection successful", timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    fun cancelKycVerification() {
        clickTag(KYC_SECTION_TAG)
        clickTag(VERIFY_KYC_BUTTON_TAG)
        waitForNode(hasText(KYC_CONFIRMATION_TITLE))
        device.pressBack()
        waitForSnackbar("KYC Verification Cancelled")
    }

    fun confirmKycVerification() {
        clickTag(VERIFY_KYC_BUTTON_TAG)
        clickText(KYC_CONFIRM_BUTTON_TEXT)
        waitForSnackbar("KYC Verification Completed", timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    fun cancelUserAttestation() {
        clickTag(USER_ATTESTATION_BUTTON_TAG)
        clickTag(USER_ATTESTATION_CANCEL_BUTTON_TAG, scrollRoot = false)
        waitForSnackbar("User Attestation cancelled")
    }

    fun confirmUserAttestation() {
        clickTag(USER_ATTESTATION_BUTTON_TAG)
        waitForNode(hasText(USER_ATTESTATION_ACCEPT_TEXT))
        clickTag(LINK_PRIMARY_BUTTON_TAG, scrollRoot = false)
        waitForSnackbar("User Attestation Confirmed", timeoutMs = 60.seconds.inWholeMilliseconds)
    }

    fun retrieveMissingTaxIdentifiers() {
        clickTag(IDENTIFIER_SECTION_TAG)
        clickTag(RETRIEVE_MISSING_IDENTIFIERS_BUTTON_TAG)
        waitForSnackbar("Missing identifiers retrieved")
        clickTag(IDENTIFIER_SECTION_TAG)
        waitForTag(MISSING_IDENTIFIERS_SUMMARY_TAG)
    }

    fun verifyEmptyTaxIdentifierIsRejected() {
        clickTag(SUBMIT_IDENTIFIERS_BUTTON_TAG)
        waitForSnackbar("Enter at least one identifier")
    }

    fun submitMaltaTaxIdentifier() {
        replaceTag("$IDENTIFIER_TYPE_TAG-0", "mt_nic")
        replaceTag("$IDENTIFIER_VALUE_TAG-0", TEST_MALTA_NATIONAL_ID)
        hideKeyboard()
        clickTag(SUBMIT_IDENTIFIERS_BUTTON_TAG)
        waitForSnackbar("Identifiers submitted", timeoutMs = 60.seconds.inWholeMilliseconds)
        clickTag(IDENTIFIER_SECTION_TAG)
        waitForTaggedText(SUBMIT_IDENTIFIERS_SUMMARY_TAG, "Completed: true")
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

    private fun acceptOAuthConsentIfShown() {
        if (waitForOptionalNode(hasTestTag(AUTHENTICATED_OPERATIONS_TAG), timeoutMs = 10.seconds.inWholeMilliseconds)) {
            return
        }

        val allowMatcher = hasText(OAUTH_ALLOW_TEXT, substring = true, ignoreCase = true)
        if (waitForOptionalNode(allowMatcher, timeoutMs = 20.seconds.inWholeMilliseconds)) {
            composeRule.onNode(allowMatcher).performClick()
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

    private fun waitForTaggedText(
        tag: String,
        text: String,
        timeoutMs: Long = defaultTimeout.inWholeMilliseconds,
    ) {
        waitForNode(
            hasTestTag(tag).and(hasText(text, substring = true, ignoreCase = true)),
            timeoutMs = timeoutMs
        )
    }

    private fun clickText(text: String, timeoutMs: Long = defaultTimeout.inWholeMilliseconds) {
        val matcher = hasText(text, substring = false, ignoreCase = true)
        waitForNode(matcher, timeoutMs)
        composeRule.onNode(matcher).performClick()
    }

    private fun replaceTag(tag: String, text: String) {
        waitForTag(tag)
        val node = composeRule.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performTextReplacement(text)
        composeRule.waitForIdle()
    }

    private fun assertEditableText(tag: String, text: String) {
        composeRule.onNodeWithTag(tag).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))
        )
    }

    private fun hideKeyboard() {
        device.pressBack()
        composeRule.waitForIdle()
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

internal data class FreshOnrampUser(
    val name: String,
    val email: String,
    val password: String,
    val phone: String,
    val country: String,
) {
    companion object {
        fun create(country: String): FreshOnrampUser {
            val uuid = UUID.randomUUID()
            val name = "$uuid-test"

            return FreshOnrampUser(
                name = name,
                email = "$name@stripe.com",
                password = E2E_PASSWORD,
                phone = uuid.testPhoneNumber(country),
                country = country,
            )
        }
    }
}

private fun UUID.testPhoneNumber(country: String): String {
    val positiveBits = leastSignificantBits ushr 1
    return when (country) {
        "MT" -> "+35679${(positiveBits % 1_000_000).toString().padStart(6, '0')}"
        else -> "+1202${((positiveBits % 8_000_000) + 2_000_000)}"
    }
}

private data class TestKycAddress(
    val city: String,
    val state: String,
    val country: String,
    val postalCode: String,
) {
    companion object {
        fun forCountry(country: String): TestKycAddress {
            return when (country) {
                "MT" -> TestKycAddress(
                    city = "Valletta",
                    state = "",
                    country = "MT",
                    postalCode = "VLT 1117",
                )
                else -> TestKycAddress(
                    city = "San Francisco",
                    state = "CA",
                    country = "US",
                    postalCode = "94111",
                )
            }
        }
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
private const val TEST_KYC_FIRST_NAME = "Onramp"
private const val TEST_KYC_ADDRESS_LINE_1 = "address_full_match"
private const val KYC_CONFIRMATION_TITLE = "Confirm your information"
private const val KYC_CONFIRM_BUTTON_TEXT = "Confirm"
private const val USER_ATTESTATION_CANCEL_BUTTON_TAG = "UserAttestationCancelButtonTag"
private const val USER_ATTESTATION_ACCEPT_TEXT = "Accept"
private const val OAUTH_ALLOW_TEXT = "Allow"
private const val TEST_MALTA_NATIONAL_ID = "1234567M"
private const val EXTRA_SCROLL_DISTANCE = 72f
private const val EXTRA_SCROLL_DURATION_MILLIS = 50L
