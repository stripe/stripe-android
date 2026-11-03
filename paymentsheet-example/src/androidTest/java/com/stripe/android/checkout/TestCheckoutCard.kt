package com.stripe.android.checkout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutElementScenarios
import com.stripe.android.test.core.runCheckoutScenario
import com.stripe.android.utils.TestRules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCheckoutCard {
    @get:Rule
    val rules = TestRules.create()

    @Test
    fun testCardPayment() = runCheckoutScenario(rules.compose, CheckoutElementScenarios.paymentElement) {
        fillNewCard()
        confirm()

        assertCompleted()
    }

    @Test
    fun testDeclinedCardShowsConfirmationFailure() =
        runCheckoutScenario(rules.compose, CheckoutElementScenarios.paymentElement) {
            fillNewCard(cardNumber = DECLINED_CARD_NUMBER)
            confirm()

            assertConfirmationFailed("Your card was declined")
        }

    private companion object {
        const val DECLINED_CARD_NUMBER = "4000000000000002"
    }
}
