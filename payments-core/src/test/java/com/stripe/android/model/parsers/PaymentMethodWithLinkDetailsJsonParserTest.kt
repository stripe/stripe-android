package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.ConsumerFixtures.CONSUMER_PAYMENT_DETAILS_JSON
import com.stripe.android.model.PaymentMethodFixtures.ALLOW_REDISPLAY_UNSPECIFIED_JSON
import org.json.JSONObject
import org.junit.Test

class PaymentMethodWithLinkDetailsJsonParserTest {

    @Test
    fun `Supports payment method that has no Link payment details`() {
        val json = JSONObject().apply {
            put("payment_method", ALLOW_REDISPLAY_UNSPECIFIED_JSON)
            // No "link_payment_details" value
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNotNull()
        assertThat(paymentMethod?.linkPaymentDetails).isNull()
    }

    @Test
    fun `Supports payment method that has Link payment details of type CARD`() {
        val linkPaymentDetails = CONSUMER_PAYMENT_DETAILS_JSON.getJSONArray("redacted_payment_details").getJSONObject(0)
        val json = JSONObject().apply {
            put("payment_method", ALLOW_REDISPLAY_UNSPECIFIED_JSON)
            put("link_payment_details", linkPaymentDetails)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNotNull()
        assertThat(paymentMethod?.linkPaymentDetails).isNotNull()
    }

    @Test
    fun `Supports payment method that has Link payment details of type BANK_ACCOUNT`() {
        val linkPaymentDetails = CONSUMER_PAYMENT_DETAILS_JSON.getJSONArray("redacted_payment_details").getJSONObject(2)
        val json = JSONObject().apply {
            put("payment_method", ALLOW_REDISPLAY_UNSPECIFIED_JSON)
            put("link_payment_details", linkPaymentDetails)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNotNull()
        assertThat(paymentMethod?.linkPaymentDetails).isNotNull()
    }

    @Test
    fun `Does not support method that has Link payment details of type other than CARD`() {
        val linkPaymentDetails = JSONObject().apply {
            put("type", "KLARNA")
        }
        val json = JSONObject().apply {
            put("payment_method", ALLOW_REDISPLAY_UNSPECIFIED_JSON)
            put("link_payment_details", linkPaymentDetails)
        }
        val paymentMethod = PaymentMethodWithLinkDetailsJsonParser.parse(json)

        assertThat(paymentMethod).isNull()
    }
}
