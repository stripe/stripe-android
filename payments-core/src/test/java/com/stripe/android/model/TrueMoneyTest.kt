package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject
import org.junit.Test

internal class TrueMoneyTest {
    @Test
    fun `creates payment method without extra required fields`() {
        assertThat(PaymentMethodCreateParams.createTrueMoney().toParamMap())
            .containsExactly("type", "truemoney")
    }

    @Test
    fun `preserves supplied billing details and metadata`() {
        val params = PaymentMethodCreateParams.createTrueMoney(
            billingDetails = PaymentMethod.BillingDetails(email = "buyer@example.com"),
            metadata = mapOf("order" to "123"),
            allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        ).toParamMap()

        assertThat(params).containsExactly(
            "type", "truemoney",
            "billing_details", mapOf("email" to "buyer@example.com"),
            "metadata", mapOf("order" to "123"),
            "allow_redisplay", "always",
        )
    }

    @Test
    fun `parses payment method type`() {
        val result = PaymentMethodJsonParser().parse(JSONObject("""{"id":"pm_123","type":"truemoney"}"""))

        assertThat(result.type).isEqualTo(PaymentMethod.Type.TrueMoney)
    }
}
