package com.stripe.android.checkout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundDefinitions
import com.stripe.android.paymentsheet.example.playground.checkout.settings.CheckoutPlaygroundSettings
import com.stripe.android.paymentsheet.example.playground.settings.Currency
import com.stripe.android.paymentsheet.example.playground.settings.Merchant
import com.stripe.android.test.core.runCheckoutTest
import com.stripe.android.utils.TestRules
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class TestCheckoutCard {
    @get:Rule
    val rules = TestRules.create()

    private val usCardOnly: CheckoutPlaygroundSettings.() -> Unit = {
        val session = CheckoutPlaygroundDefinitions.session
        update(session.merchant, Merchant.US)
        update(session.currency, Currency.USD)
        update(session.automaticPaymentMethods, false)
        update(session.allowedPaymentMethodTypes, listOf(PaymentMethod.Type.Card.code))
    }

    @Test
    fun testCardPayment() = runCheckoutTest(rules.compose, usCardOnly) {
        fillNewCard()
        confirm()

        assertCompleted()
    }

    @Test
    fun testDeclinedCardShowsConfirmationFailure() = runCheckoutTest(rules.compose, usCardOnly) {
        fillNewCard(cardNumber = DECLINED_CARD_NUMBER)
        confirm()

        assertConfirmationFailed("Your card was declined")
    }

    private companion object {
        const val DECLINED_CARD_NUMBER = "4000000000000002"
    }
}
