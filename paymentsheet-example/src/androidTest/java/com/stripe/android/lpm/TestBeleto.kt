package com.stripe.android.lpm

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseLpmTest
import com.stripe.android.test.core.AuthorizeAction
import com.stripe.android.test.core.Currency
import com.stripe.android.test.core.DelayedPMs
import com.stripe.android.test.core.IntentType
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestBoleto : BaseLpmTest() {
    private val boleto = newUser.copy(
        paymentMethod = lpmRepository.fromCode("boleto")!!,
        currency = Currency.BRL,
        merchantCountryCode = "BR",
        delayed = DelayedPMs.On,
    )

    @Test
    fun testBoleto() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = boleto,
        )
    }

    @Test
    fun testBoletoSfu() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = boleto.copy(
                intentType = IntentType.PayWithSetup,
            ),
        )
    }

    @Test
    fun testBoletoSetup() {
        testDriver.confirmNewOrGuestComplete(
            testParameters = boleto.copy(
                intentType = IntentType.Setup,
                authorizationAction = AuthorizeAction.AuthorizeSetup,
            ),
        )
    }

    @Test
    fun testBoletoInCustomFlow() {
        testDriver.confirmCustom(
            testParameters = boleto,
        )
    }
}
