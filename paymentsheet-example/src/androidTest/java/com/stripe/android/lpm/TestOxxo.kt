package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestOxxo : BaseLpmTest() {
    private val oxxo = newUser.copy(
        customer = Customer.Guest,
        paymentMethod = lpmRepository.fromCode("oxxo")!!,
        currency = Currency.MXN,
        merchantCountryCode = "MX",
        delayed = DelayedPMs.On,
        billing = Billing.Off,
        authorizationAction = AuthorizeAction.DisplayQrCode,
    )

    @Test
    fun testOxxo() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = oxxo,
        )
    }

    @Test
    fun testOxxoInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = oxxo,
        )
    }
}
