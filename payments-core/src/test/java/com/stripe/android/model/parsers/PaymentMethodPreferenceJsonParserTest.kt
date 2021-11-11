package com.stripe.android.model.parsers

import com.google.common.truth.Truth
import com.stripe.android.model.PaymentMethodPreferenceFixtures
import org.junit.Test

class PaymentMethodPreferenceJsonParserTest {
    @Test
    fun parsePaymentIntent_shouldCreateObjectWithOrderedPaymentMethods() {
        val paymentIntent = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_JSON
        )
        val orderedPaymentMethods =
            ModelJsonParser.jsonArrayToList(
                PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_JSON
                    .optJSONObject("payment_method_preference")!!
                    .optJSONArray("ordered_payment_method_types")
            )

        Truth.assertThat(paymentIntent?.id)
            .isEqualTo("pi_3JTDhYIyGgrkZxL71IDUGKps")
        Truth.assertThat(paymentIntent?.paymentMethodTypes)
            .containsExactlyElementsIn(orderedPaymentMethods)
            .inOrder()
    }

    @Test
    fun parseSetupIntent_shouldCreateObjectWithOrderedPaymentMethods() {
        val setupIntent = PaymentMethodPreferenceForSetupIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_JSON
        )
        val orderedPaymentMethods =
            ModelJsonParser.jsonArrayToList(
                PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_JSON
                    .optJSONObject("payment_method_preference")!!
                    .optJSONArray("ordered_payment_method_types")
            )

        Truth.assertThat(setupIntent?.id)
            .isEqualTo("seti_1JTDqGIyGgrkZxL7reCXkpr5")
        Truth.assertThat(setupIntent?.paymentMethodTypes)
            .containsExactlyElementsIn(orderedPaymentMethods)
            .inOrder()
    }
}
