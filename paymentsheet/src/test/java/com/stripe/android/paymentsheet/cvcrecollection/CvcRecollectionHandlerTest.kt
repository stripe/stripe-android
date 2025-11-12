package com.stripe.android.paymentsheet.cvcrecollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import org.junit.Test

class CvcRecollectionHandlerTest {
    private val handler = CvcRecollectionHandlerImpl()

    @Test
    fun testLaunch() {
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

        val expected = CvcRecollectionData.fromPaymentSelection(paymentMethod.card)

        handler.launch(paymentMethod) { actual ->
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `card payment method and intent requiring cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = paymentMethod,
            optionsParams = null,
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `card payment method and intent not requiring cvc recollection should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `non-card payment method and intent requiring cvc recollection should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD,
            optionsParams = null,
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `card & intent requiring cvc recollection should return true if card is from a wallet`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.run {
            copy(card = card?.copy(wallet = Wallet.GooglePayWallet(dynamicLast4 = null)))
        }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = paymentMethod,
            optionsParams = null,
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `non-card payment method should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            optionsParams = null,
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `cvcRecEnabled - cvc rec payment intent, cvc rec config disabled, and payment intent returned true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `cvcRecEnabled - no cvc rec payment intent, cvc rec config disabled, and payment intent returned false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
        )
        assertThat(response).isFalse()
    }
}
