package com.stripe.android.crypto.onramp.example

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class OnrampFlowTest {
    @get:Rule
    val onrampRule = OnrampE2ETestRule()

    private val page by lazy { OnrampE2EPage(onrampRule.composeRule) }

    @Test
    fun cardCollectionCanBeCanceledAndRetried() {
        page.loginAndAuthenticateWithOtp()
        page.cancelCardCollection()
        page.collectExistingCard()
    }

    @Test
    fun seamlessSignInLogoutAndReauthentication() {
        page.loginAndAuthenticateWithOtp()
        page.returnToSeamlessSignIn()
        page.declineSeamlessSignIn()
        page.loginAndAuthenticateWithOtp()
        page.logOut()
    }

    @Test
    fun bankAccountCheckoutWithStandardSettlement() {
        page.loginAndAuthenticateWithOtp()
        page.registerDefaultWallet()
        page.collectBankAccount()
        page.createPaymentTokenAndSession()
        page.performCheckout()
        page.waitForCheckoutCompleted()
    }

    @Test
    fun checkoutSurvivesActivityRecreation() {
        page.loginAndAuthenticateWithOtp()
        page.registerDefaultWallet()
        page.collectExistingCard()
        page.createPaymentTokenAndSession()

        onrampRule.recreateHostActivity()

        page.performCheckout()
        page.waitForCheckoutCompleted()
    }

    @Test
    fun freshUserKycCanBeCollectedAndVerifiedAfterCancellation() {
        val user = page.registerAndAuthenticateFreshUser(country = "US")

        page.collectKycInfo(user)
        page.cancelKycVerification()
        page.confirmKycVerification()
    }

    @Test
    fun freshUserCanCompleteIdentityVerificationAndCheckout() {
        val user = page.registerAndAuthenticateFreshUser(country = "US")

        page.collectKycInfo(user)
        page.confirmKycVerification()
        page.completeIdentityVerification()
        page.registerDefaultWallet()
        page.collectNewCard()
        page.createPaymentTokenAndSession()
        page.performCheckout()
        page.waitForCheckoutCompleted()
    }

    @Test
    fun freshUserIdentityVerificationCanFailAndBeRetried() {
        val user = page.registerAndAuthenticateFreshUser(country = "US")

        page.collectKycInfo(user)
        page.confirmKycVerification()
        page.failIdentityVerification()
        page.loginAndAuthenticateWithOtp(user.email, user.password)
        page.completeIdentityVerification()
    }

    @Test
    fun freshUserAttestationCanBeCanceledAndConfirmed() {
        val user = page.registerAndAuthenticateFreshUser(country = "MT")

        page.collectKycInfo(user)
        page.retrieveMissingTaxIdentifiers()
        page.submitMaltaTaxIdentifier()
        page.cancelUserAttestation()
        page.confirmUserAttestation()
    }

    @Test
    fun freshUserTaxIdentifiersCanBeValidatedAndSubmitted() {
        val user = page.registerAndAuthenticateFreshUser(country = "MT")

        page.collectKycInfo(user)
        page.retrieveMissingTaxIdentifiers()
        page.verifyEmptyTaxIdentifierIsRejected()
        page.submitMaltaTaxIdentifier()
    }

    @Test
    fun freshUserSolanaWalletOwnershipCanBeVerified() {
        page.registerAndAuthenticateFreshUser(country = "US")

        page.registerSolanaWallet()
        onrampRule.verifySolanaWalletOwnership()
    }
}
