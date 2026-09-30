package com.stripe.android.crypto.onramp.example

import android.app.Instrumentation
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.crypto.onramp.example.model.Screen
import com.stripe.android.crypto.onramp.model.KycInfo
import com.stripe.android.crypto.onramp.model.LinkUserInfo
import com.stripe.android.crypto.onramp.model.OnrampAttachKycInfoResult
import com.stripe.android.crypto.onramp.model.OnrampCreateCryptoPaymentTokenResult
import com.stripe.android.crypto.onramp.model.OnrampHasLinkAccountResult
import com.stripe.android.crypto.onramp.model.OnrampRegisterLinkUserResult
import com.stripe.android.crypto.onramp.model.PaymentMethodDisplayData
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/** Runs the headless new-user path against the live test backend after real Google Pay collection. */
internal fun OnrampE2ETestRule.registerWalletUserAndCreateTokenWithoutOtp() {
    val wallet = uiState()
    val walletEmail = requireNotNull(wallet.walletEmail)
    // A stable Google account email would be a returning Link user after the first run.
    // Only the email is aliased; phone, name and address come directly from the wallet.
    val email = walletEmail.substringBefore('@').substringBefore('+').take(20) +
        "+onramp-${UUID.randomUUID().toString().replace("-", "")}@${walletEmail.substringAfter('@')}"
    assertThat(wallet.kycFirstName).isNotEmpty()
    assertThat(wallet.kycLastName).isNotEmpty()
    assertThat(wallet.kycAddress.country).isEqualTo("US")
    assertThat(wallet.kycAddress.line1).isNotEmpty()
    assertThat(wallet.kycAddress.city).isNotEmpty()
    assertThat(wallet.kycAddress.postalCode).isNotEmpty()
    val info = LinkUserInfo(
        email = email,
        fullName = "${wallet.kycFirstName} ${wallet.kycLastName}",
        phone = requireNotNull(wallet.walletPhone),
        country = requireNotNull(wallet.kycAddress.country),
    )
    val kycInfo = KycInfo(
        firstName = wallet.kycFirstName,
        lastName = wallet.kycLastName,
        idNumber = null,
        dateOfBirth = null,
        address = wallet.kycAddress,
    )
    val coordinator = onrampCoordinator()
    val launches = AtomicInteger()
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val monitor = object : Instrumentation.ActivityMonitor() {
        override fun onStartActivity(intent: Intent): Instrumentation.ActivityResult? {
            launches.incrementAndGet()
            return null
        }
    }
    instrumentation.addMonitor(monitor)
    try {
        runBlocking {
            withTimeout(120_000) {
                when (val lookup = coordinator.hasLinkAccount(info.email)) {
                    is OnrampHasLinkAccountResult.Completed -> assertThat(lookup.hasLinkAccount).isFalse()
                    is OnrampHasLinkAccountResult.Failed -> throw AssertionError("Link lookup failed", lookup.error)
                }
                when (val registration = coordinator.registerLinkUser(info)) {
                    is OnrampRegisterLinkUserResult.Completed -> assertThat(registration.customerId).isNotEmpty()
                    is OnrampRegisterLinkUserResult.Failed ->
                        throw AssertionError("Wallet-contact registration failed", registration.error)
                }
                when (val attachment = coordinator.attachKycInfo(kycInfo)) {
                    is OnrampAttachKycInfoResult.Completed -> Unit
                    is OnrampAttachKycInfoResult.Failed -> throw AssertionError("KYC submission failed", attachment.error)
                }
                // No authorize, token authentication, OTP, or second wallet collection call.
                when (val token = coordinator.createCryptoPaymentToken()) {
                    is OnrampCreateCryptoPaymentTokenResult.Completed ->
                        assertThat(token.cryptoPaymentToken).isNotEmpty()
                    is OnrampCreateCryptoPaymentTokenResult.Failed ->
                        throw AssertionError("Token creation without OTP failed", token.error)
                }
            }
        }
        instrumentation.waitForIdleSync()
        assertThat(launches.get()).isEqualTo(0)
        val state = uiState()
        assertThat(state.screen).isEqualTo(Screen.LoginSignup)
        assertThat(state.authToken).isNull()
        assertThat(state.linkAuthIntentId).isNull()
        assertThat(state.selectedPaymentData).isSameInstanceAs(wallet.selectedPaymentData)
        assertThat(state.selectedPaymentData?.type).isEqualTo(PaymentMethodDisplayData.Type.GooglePay)
    } finally {
        instrumentation.removeMonitor(monitor)
    }
}
