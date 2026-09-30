package com.stripe.android.crypto.onramp.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Live E2E coverage using the actual Google Pay sheet and the onramp test backend.
 * Requires a signed-in Google Play device with a test wallet and complete US billing contact.
 */
@RunWith(AndroidJUnit4::class)
internal class OnrampPlatformPayFlowTest {
    @get:Rule
    val onrampRule = OnrampE2ETestRule(retryCount = 1)

    private val page by lazy { OnrampE2EPage(onrampRule.composeRule) }

    @Before
    fun startWithoutLinkSession() {
        // The SDK component survives activity teardown; clearing the example preferences alone
        // does not clear the previous test's crypto customer or Link session.
        page.waitForLogin()
        onrampRule.clearLinkSession()
    }

    @After
    fun clearLinkSession() {
        onrampRule.clearLinkSession()
    }

    @Test
    fun googlePayBeforeLinkOtpCanCreatePaymentToken() {
        page.collectGooglePayBeforeAuthentication()
        onrampRule.assertWalletCollectedBeforeAuthentication()

        page.loginAndAuthenticateWithOtp()
        page.waitForSelectedPayment()
        page.createPaymentToken()

        onrampRule.assertGooglePayTokenCreated()
    }

    @Test
    fun googlePayContactsCanRegisterAndCreateTokenWithoutOtp() {
        page.collectGooglePayBeforeAuthentication()
        onrampRule.assertWalletCollectedBeforeAuthentication()

        onrampRule.registerWalletUserAndCreateTokenWithoutOtp()

        // The direct SDK flow completes while the merchant's login screen stays visible.
        onrampRule.composeRule.onNodeWithTag(LOGIN_LOGIN_BUTTON_TAG).assertIsDisplayed()
        onrampRule.composeRule.onNodeWithTag("OTP-0").assertDoesNotExist()
    }

    @Test
    fun googlePayBeforeRegistrationAndKycCanCreatePaymentToken() {
        page.collectGooglePayBeforeAuthentication()
        onrampRule.assertWalletCollectedBeforeAuthentication()

        val user = page.registerAndAuthenticateFreshUser(country = "US")
        page.collectKycInfo(user)
        page.confirmKycVerification()
        page.completeIdentityVerification()
        // Do not collect another payment method: token creation must use the pre-auth wallet.
        page.waitForSelectedPayment()
        page.createPaymentToken()

        onrampRule.assertGooglePayTokenCreated()
    }
}
