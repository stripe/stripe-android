package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.model.PaymentMethod
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Currency
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestAlipay : BaseLpmTest() {

    private val alipay = newUser.copy(
        paymentMethod = lpmRepository.fromCode("alipay")!!,
        currency = Currency.USD,
        merchantCountryCode = "US",
        authorizationAction = AuthorizeAction.AuthorizePayment,
        supportedPaymentMethods = listOf(
            PaymentMethod.Type.Card.code,
            PaymentMethod.Type.Klarna.code,
            PaymentMethod.Type.Affirm.code,
            PaymentMethod.Type.Alipay.code,
        ),
    )

    @Test
    fun testAlipay() {
        testDriver.confirmNewOrGuestComplete(alipay)
    }

    @Test
    fun testAlipayFailure() {
        testDriver.confirmNewOrGuestComplete(
            alipay.copy(
                authorizationAction = AuthorizeAction.Fail(
                    expectedError = "We are unable to authenticate your payment method. Please " +
                        "choose a different payment method and try again.",
                ),
            )
        )
    }

    @Test
    fun testAlipayInCustomFlow() {
        testDriver.confirmCustom(alipay)
    }
}
