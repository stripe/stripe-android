package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject
import org.junit.Test

internal class NgCardTest {
    @Test
    fun `creates payment method without extra required fields`() {
        assertThat(PaymentMethodCreateParams.createNgCard().toParamMap()).containsExactly("type", "ng_card")
    }

    @Test
    fun `preserves supplied billing details and metadata`() {
        val params = PaymentMethodCreateParams.createNgCard(
            billingDetails = PaymentMethod.BillingDetails(email = "buyer@example.com"),
            metadata = mapOf("order" to "123"),
            allowRedisplay = PaymentMethod.AllowRedisplay.ALWAYS,
        ).toParamMap()

        assertThat(params).containsExactly(
            "type", "ng_card",
            "billing_details", mapOf("email" to "buyer@example.com"),
            "metadata", mapOf("order" to "123"),
            "allow_redisplay", "always",
        )
    }

    @Test
    fun `parses payment method type`() {
        val result = PaymentMethodJsonParser().parse(JSONObject("""{"id":"pm_123","type":"ng_card"}"""))

        assertThat(result.type).isEqualTo(PaymentMethod.Type.NgCard)
    }
}
