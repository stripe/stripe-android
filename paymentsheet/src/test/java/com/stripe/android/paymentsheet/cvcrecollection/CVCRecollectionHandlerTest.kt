package com.stripe.android.paymentsheet.cvcrecollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.CvcRecollectionCallbackHandler
import com.stripe.android.paymentsheet.CvcRecollectionEnabledCallback
import com.stripe.android.paymentsheet.ExperimentalCvcRecollectionApi
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.model.PaymentSelection
import org.junit.Test

@OptIn(ExperimentalCvcRecollectionApi::class)
class CVCRecollectionHandlerTest {
    private val handler = CVCRecollectionHandlerImpl()

    @Test
    fun `saved card payment selection and intent requiring cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD_CVC_RECOLLECTION
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `saved card payment selection and intent not requiring cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `saved non-card payment selection and intent requiring cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.PAYPAL_PAYMENT_METHOD
        )
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("")
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
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `saved card payment selection with deferred init and enabled deferred cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        CvcRecollectionCallbackHandler.isCvcRecollectionEnabledCallback = CvcRecollectionEnabledCallback { true }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    )
                )
            )
        )
        assertThat(response).isTrue()
    }

    @Test
    fun `saved card payment selection with non-deferred init and enabled deferred cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        )
        CvcRecollectionCallbackHandler.isCvcRecollectionEnabledCallback = CvcRecollectionEnabledCallback { true }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent("")
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `non-saved card payment selection with deferred init and enabled deferred cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.GooglePay
        CvcRecollectionCallbackHandler.isCvcRecollectionEnabledCallback = CvcRecollectionEnabledCallback { true }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    )
                )
            )
        )
        assertThat(response).isFalse()
    }

    @Test
    fun `saved non-card payment selection with deferred init and enabled deferred cvc recollection should return true`() {
        val paymentIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD
        val paymentSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFixtures.SEPA_DEBIT_PAYMENT_METHOD
        )
        CvcRecollectionCallbackHandler.isCvcRecollectionEnabledCallback = CvcRecollectionEnabledCallback { true }
        val response = handler.requiresCVCRecollection(
            stripeIntent = paymentIntent,
            paymentSelection = paymentSelection,
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 1234,
                        currency = "cad",
                    )
                )
            )
        )
        assertThat(response).isFalse()
    }
}