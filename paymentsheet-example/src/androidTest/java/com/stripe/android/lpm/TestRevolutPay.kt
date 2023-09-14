package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.IntentType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestRevolutPay : BaseLpmTest() {
    private val revolutPay = newUser.copy(
        paymentMethod = lpmRepository.fromCode("revolut_pay")!!,
        currency = Currency.GBP,
        merchantCountryCode = "GB",
        authorizationAction = AuthorizeAction.AuthorizePayment,
        supportedPaymentMethods = listOf("card", "revolut_pay"),
    )

    @Test
    fun testRevolutPay_Success() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = revolutPay,
        )
    }

    @Test
    fun testRevolutPay_Fail() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = revolutPay.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "The customer declined this payment.",
                ),
            ),
        )
    }

    @Test
    fun testRevolutPay_Cancel() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = revolutPay.copy(
                authorizationAction = AuthorizeAction.Cancel,
            ),
        )
    }

    @Test
    fun testRevolutPayWithSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = revolutPay.copy(
                intentType = IntentType.PayWithSetup,
                authorizationAction = AuthorizeAction.AuthorizePayment,
            ),
        )
    }

    @Test
    fun testRevolutPayWithSetupIntent() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = revolutPay.copy(
                intentType = IntentType.Setup,
            ),
        )
    }

    @Test
    fun testRevolutPayInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = revolutPay,
        )
    }
}
