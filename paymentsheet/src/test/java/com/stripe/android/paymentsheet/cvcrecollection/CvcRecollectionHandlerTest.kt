package com.stripe.android.paymentsheet.cvcrecollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.cvcrecollection.CvcRecollectionData
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import org.junit.Test

class CvcRecollectionHandlerTest {
    private val handler = CvcRecollectionHandlerImpl()

    @Test
    fun testLaunch() {
        val expected = CvcRecollectionData.fromPaymentSelection(PaymentMethodFixtures.CARD_PAYMENT_METHOD.card)

        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )

        handler.launch(paymentSelection) { actual ->
            assertThat(actual).isEqualTo(expected)
        }
    }

    @Test
    fun `saved card payment selection and intent requiring cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `valid card, payment intent and false extraRequirements should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(""),
            extraRequirements = { false }
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `saved card payment selection and intent not requiring cvc recollection should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `saved non-card payment selection and intent requiring cvc recollection should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `non-saved payment selection and intent requiring cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentSelection = PaymentSelection.GooglePay
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `saved card payment selection with deferred init and enabled deferred cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
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
    fun `valid card, deferred init and  and false extraRequirements should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    ),
                    requireCvcRecollection = true
                )
            ),
            extraRequirements = { false }
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `valid card selection with non-deferred init and enabled deferred cvc recollection should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `non-saved card payment selection with valid deferred intent should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.GooglePay
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
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
    fun `saved non-card payment selection with valid deferred intent should return false`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
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
