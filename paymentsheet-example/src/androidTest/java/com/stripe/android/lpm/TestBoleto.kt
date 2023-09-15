package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Billing
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.Customer
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.FieldPopulator
import com.stripe.android.test.core.IntentType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBoleto : BaseLpmTest() {
    private val boleto = newUser.copy(
        customer = Customer.Guest,
        paymentMethod = lpmRepository.fromCode("boleto")!!,
        currency = Currency.BRL,
        merchantCountryCode = "BR",
        delayed = DelayedPMs.On,
        billing = Billing.Off,
        authorizationAction = AuthorizeAction.DisplayQrCode,
    )

    private val boletoValues = FieldPopulator.Values().copy(
        zip = "76600-000",
        state = "GO",
    )

    @Test
    fun testBoleto() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = boleto,
            values = boletoValues,
        )
    }

    @Test
    fun testBoletoSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = boleto.copy(
                intentType = IntentType.PayWithSetup,
            ),
            values = boletoValues,
        )
    }

    @Test
    fun testBoletoSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = boleto.copy(
                intentType = IntentType.Setup,
            ),
            values = boletoValues,
        )
    }

    @Test
    fun testBoletoInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = boleto,
            values = boletoValues,
        )
    }
}
