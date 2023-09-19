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
internal class TestKonbini : BaseLpmTest() {
    private val konbini = newUser.copy(
        customer = Customer.Guest,
        paymentMethod = lpmRepository.fromCode("konbini")!!,
        currency = Currency.JPY,
        merchantCountryCode = "JP",
        delayed = DelayedPMs.On,
        billing = Billing.Off,
        authorizationAction = AuthorizeAction.DisplayQrCode,
    )

    @Test
    fun testKonbini() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = konbini,
        )
    }

    @Test
    fun testKonbiniInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = konbini,
        )
    }
}
