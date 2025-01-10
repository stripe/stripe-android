package com.stripe.android.paymentsheet.cvcrecollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.model.wallets.Wallet
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.state.PaymentElementLoader
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
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
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
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
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
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `card & intent requiring cvc recollection should return false if CVC is in options params`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = paymentMethod,
            optionsParams = PaymentMethodOptionsParams.Card(cvc = "444"),
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `card & intent requiring cvc recollection should return false if card is from a wallet`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.run {
            copy(card = card?.copy(wallet = Wallet.GooglePayWallet(dynamicLast4 = null)))
        }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = paymentMethod,
            optionsParams = null,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `card payment method with deferred init and enabled deferred cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `valid card with non-deferred init and enabled deferred cvc recollection should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = null,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `non-card payment method with valid deferred intent should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD,
            optionsParams = null,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `card & valid deferred intent should return false if CVC is in options params`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            optionsParams = PaymentMethodOptionsParams.Card(cvc = "444"),
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `card & valid deferred intent should return false if card is from a wallet`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.run {
            copy(card = card?.copy(wallet = Wallet.GooglePayWallet(dynamicLast4 = null)))
        }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentMethod = paymentMethod,
            optionsParams = null,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `cvcRecEnabled - cvc rec payment intent, cvc rec config enabled, and deferred intent returned true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `cvcRecEnabled - cvc rec payment intent, cvc rec config disabled, and payment intent returned true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `cvcRecEnabled - no cvc rec payment intent, cvc rec config enabled, and deferred intent returned true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `cvcRecEnabled - no cvc rec payment intent, cvc rec config disabled, and deferred intent returned true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = false
                )
            )
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `cvcRecEnabled - cvc rec config enabled, initialization mode as Setup returns false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Setup(),
                    requireCvcRecollection = true
                )
            )
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `cvcRecEnabled - no cvc rec payment intent, cvc rec config disabled, and payment intent returned false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD

        val response = handler.cvcRecollectionEnabled(
            stripeIntent = paymentIntent,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }
}
