package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PaymentIntentFixtures.PI_WITH_CARD_AFTERPAY_AU_BECS
import com.stripe.android.model.PaymentIntentFixtures.PI_WITH_CARD_AFTERPAY_AU_BECS_NO_ORDERED_LPMS
import com.stripe.android.model.PaymentMethodPreferenceFixtures
import com.stripe.android.model.countryCode
import org.json.JSONObject
import org.junit.Test

class PaymentMethodPreferenceJsonParserTest {
    @Test
    fun parsePaymentIntent_shouldCreateObjectWithOrderedPaymentMethods() {
        val paymentMethodPreference = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_JSON
        )
        val orderedPaymentMethods =
            ModelJsonParser.jsonArrayToList(
                PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_JSON
                    .optJSONObject("payment_method_preference")!!
                    .optJSONArray("ordered_payment_method_types")
            )

        assertThat(paymentMethodPreference?.intent?.id)
            .isEqualTo("pi_3JTDhYIyGgrkZxL71IDUGKps")
        assertThat(paymentMethodPreference?.intent?.paymentMethodTypes)
            .containsExactlyElementsIn(orderedPaymentMethods)
            .inOrder()
    }

    @Test
    fun parseSetupIntent_shouldCreateObjectWithOrderedPaymentMethods() {
        val paymentMethodPreference = PaymentMethodPreferenceForSetupIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_JSON
        )
        val orderedPaymentMethods =
            ModelJsonParser.jsonArrayToList(
                PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_JSON
                    .optJSONObject("payment_method_preference")!!
                    .optJSONArray("ordered_payment_method_types")
            )

        assertThat(paymentMethodPreference?.intent?.id)
            .isEqualTo("seti_1JTDqGIyGgrkZxL7reCXkpr5")
        assertThat(paymentMethodPreference?.intent?.paymentMethodTypes)
            .containsExactlyElementsIn(orderedPaymentMethods)
            .inOrder()
    }

    @Test
    fun parsePaymentIntent_shouldCreateObjectLinkFundingSources() {
        val paymentMethodPreference = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_WITH_LINK_FUNDING_SOURCES_JSON
        )!!

        assertThat(paymentMethodPreference.intent.linkFundingSources)
            .containsExactly("card", "bank_account")
    }

    @Test
    fun parseSetupIntent_shouldCreateObjectLinkFundingSources() {
        val paymentMethodPreference = PaymentMethodPreferenceForSetupIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_WITH_LINK_FUNDING_SOURCES_JSON
        )!!

        assertThat(paymentMethodPreference.intent.linkFundingSources)
            .containsExactly("card", "bank_account")
    }

    @Test
    fun `Test ordered payment methods returned in PI payment_method_type variable`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            JSONObject(
                PI_WITH_CARD_AFTERPAY_AU_BECS
            )
        )
        assertThat(parsedData?.intent?.paymentMethodTypes).isEqualTo(
            listOf(
                "au_becs_debit",
                "afterpay_clearpay",
                "card"
            )
        )
    }

    @Test
    fun `Test ordered payment methods not required in response`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            JSONObject(
                PI_WITH_CARD_AFTERPAY_AU_BECS_NO_ORDERED_LPMS
            )
        )
        // This is the order in the original payment intent
        assertThat(parsedData?.intent?.paymentMethodTypes).isEqualTo(
            listOf(
                "card",
                "afterpay_clearpay",
                "au_becs_debit"
            )
        )
    }

    @Test
    fun `Test ordered payment methods is not required`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            JSONObject(
                """
                    {
                      "payment_method_preference": {
                         "object": "payment_method_preference",
                         "payment_intent": {
                         }
                      }
                    }
                """.trimIndent()
            )
        )
        assertThat(parsedData).isNull()
    }

    @Test
    fun `Test fail to parse the payment intent`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            JSONObject(
                """
                    {
                      "payment_method_preference": {
                         "object": "payment_method_preference",
                         "payment_intent": {
                         }
                      }
                    }
                """.trimIndent()
            )
        )
        assertThat(parsedData).isNull()
    }

    @Test
    fun `Test fail to find the payment intent`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            JSONObject(
                """
                    {
                      "payment_method_preference": {
                         "object": "payment_method_preference"
                      }
                    }
                """.trimIndent()
            )
        )
        assertThat(parsedData).isNull()
    }

    @Test
    fun `Test PI with country code`() {
        val parsedData = PaymentMethodPreferenceForPaymentIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_PAYMENT_INTENT_JSON
        )
        assertThat(parsedData?.intent?.countryCode).isEqualTo("US")
    }

    @Test
    fun `Test SI with country code`() {
        val parsedData = PaymentMethodPreferenceForSetupIntentJsonParser().parse(
            PaymentMethodPreferenceFixtures.EXPANDED_SETUP_INTENT_JSON
        )
        assertThat(parsedData?.intent?.countryCode).isEqualTo("US")
    }
}
