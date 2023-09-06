package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.IntentType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCashApp : BaseLpmTest() {

    private val cashApp = newUser.copy(
        paymentMethod = lpmRepository.fromCode("cashapp")!!,
        currency = Currency.USD,
        merchantCountryCode = "US",
        authorizationAction = AuthorizeAction.AuthorizePayment,
        supportedPaymentMethods = listOf("card", "cashapp"),
    )

    @Test
    fun testCashAppPay_Success() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = cashApp,
        )
    }

    @Test
    fun testCashAppPay_Fail() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = cashApp.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "The customer declined this payment.",
                ),
            ),
        )
    }

    @Test
    fun testCashAppPay_Cancel() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = cashApp.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
        )
    }

    @Test
    fun testCashAppPayWithSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = cashApp.copy(
                intentType = IntentType.PayWithSetup,
                authorizationAction = AuthorizeAction.AuthorizePayment,
            ),
        )
    }

    @Test
    fun testCashAppPayWithSetupIntent() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = cashApp.copy(
                intentType = IntentType.Setup,
                authorizationAction = AuthorizeAction.AuthorizeSetup,
            ),
        )
    }

    @Test
    fun testCashAppPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = cashApp,
        )
    }
}
