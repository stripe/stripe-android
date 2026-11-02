package com.stripe.android.checkout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.BaseCheckoutTest
import com.stripe.android.leaf
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutCustomer
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutElementScenarios
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCheckoutCard : BaseCheckoutTest() {
    private val settings = checkoutSettings(CheckoutElementScenarios.group.leaf("payment_element")) {
        update(CheckoutPlaygroundDefinitions.session.customer, CheckoutCustomer.Guest)
    }

    @Test
    fun testCardPayment() {
        testDriver.launch(settings)
        testDriver.fillNewCard()
        testDriver.confirm()

        testDriver.assertCompleted()
    }

    @Test
    fun testDeclinedCardShowsConfirmationFailure() {
        testDriver.launch(settings)
        testDriver.fillNewCard(cardNumber = DECLINED_CARD_NUMBER)
        testDriver.confirm()

        testDriver.assertConfirmationFailed("Your card was declined")
    }

    private companion object {
        const val DECLINED_CARD_NUMBER = "4000000000000002"
    }
}
