package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Currency
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestGrabPay : BaseLpmTest() {

    private val grabPay = newUser.copy(
        paymentMethod = lpmRepository.fromCode("grabpay")!!,
        currency = Currency.SGD,
        merchantCountryCode = "SG",
        authorizationAction = AuthorizeAction.AuthorizePayment,
    )

    @Test
    fun testGrabPay() {
        testDriver.confirmNewOrGuestComplete(grabPay)
    }

    @Test
    fun testGrabPayFailure() {
        testDriver.confirmNewOrGuestComplete(
            grabPay.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "We are unable to authenticate your payment method. Please " +
                        "choose a different payment method and try again.",
                ),
            )
        )
    }

    @Test
    fun testGrabPayInCustomFlow() {
        testDriver.confirmCustom(grabPay)
    }
}
