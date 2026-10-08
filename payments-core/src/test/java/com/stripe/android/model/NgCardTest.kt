package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
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

    @Test
    fun `parses known Naira card brands and last four digits`(
        @TestParameter(value = ["amex", "mastercard", "verve", "visa", "VISA"]) brand: String,
    ) {
        val result = PaymentMethodJsonParser().parse(
            JSONObject("""{"id":"pm_123","type":"ng_card","ng_card":{"brand":"$brand","last4":"1234"}}"""),
        )
        assertThat(result.ngCard?.brand?.code).isEqualTo(brand.lowercase())
        assertThat(result.ngCard?.last4).isEqualTo("1234")
    }

    @Test
    fun `handles missing and unknown card details`(
        @TestParameter(value = ["{}", "{\"brand\":\"future_brand\",\"last4\":null}"]) details: String,
    ) {
        val result = PaymentMethodJsonParser().parse(
            JSONObject("""{"id":"pm_123","type":"ng_card","ng_card":$details}"""),
        )
        assertThat(result.ngCard?.brand).isEqualTo(PaymentMethod.NgCard.Brand.Unknown)
        assertThat(result.ngCard?.last4).isNull()
    }

    @Test
    fun `missing details remain null`() {
        val result = PaymentMethodJsonParser().parse(JSONObject("""{"id":"pm_123","type":"ng_card"}"""))
        assertThat(result.ngCard).isNull()
    }
}
