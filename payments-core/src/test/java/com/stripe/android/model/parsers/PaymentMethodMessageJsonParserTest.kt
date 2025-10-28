package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessageFixtures
import org.json.JSONObject
import org.junit.Test

class PaymentMethodMessageJsonParserTest {

    @Test
    fun parsesNoContent() {
        val message = PaymentMethodMessageJsonParser().parse(PaymentMethodMessageFixtures.NO_CONTENT_JSON)
        assertThat(message).isNotNull()
        assertThat(message?.paymentMethods).isEmpty()
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNull()
    }

    @Test
    fun parsesSinglePartner() {
        val message = PaymentMethodMessageJsonParser().parse(PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON)
        assertThat(message).isNotNull()
        assertThat(message?.paymentMethods).hasSize(1)
        assertThat(message?.singlePartner).isNotNull()
        assertThat(message?.singlePartner?.inlinePartnerPromotion).isNotNull()
        assertThat(message?.singlePartner?.learnMore).isNotNull()
        assertThat(message?.singlePartner?.darkImage).isNotNull()
        assertThat(message?.singlePartner?.lightImage).isNotNull()
        assertThat(message?.singlePartner?.flatImage).isNotNull()
    }

    @Test
    fun parsesMultiPartner() {
        val message = PaymentMethodMessageJsonParser().parse(PaymentMethodMessageFixtures.MULTI_PARTNER_JSON)
        assertThat(message).isNotNull()
        assertThat(message?.paymentMethods).hasSize(3)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNotNull()
        assertThat(message?.multiPartner?.lightImages).hasSize(3)
        assertThat(message?.multiPartner?.darkImages).hasSize(3)
        assertThat(message?.multiPartner?.darkImages).hasSize(3)
        assertThat(message?.multiPartner?.learnMore).isNotNull()
    }

    @Test
    fun handlesInvalidJsonGracefully() {
        val message = PaymentMethodMessageJsonParser().parse(JSONObject("{}"))
        assertThat(message?.paymentMethods).isEmpty()
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNull()
    }

    @Test
    fun `returns null if learn more missing multi partner`() {
        val invalidMessage = PaymentMethodMessageFixtures.MULTI_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONObject("content")
            .remove("learn_more")
        val message = PaymentMethodMessageJsonParser().parse(invalidMessage)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNull()
    }

    @Test
    fun `returns null if promotion missing multi partner`() {
        val invalidMessage = PaymentMethodMessageFixtures.MULTI_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONObject("content")
            .remove("promotion")
        val message = PaymentMethodMessageJsonParser().parse(invalidMessage)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNull()
    }

    @Test
    fun `falls back to multi partner if single partner learn more missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("learn_more")
        val message = PaymentMethodMessageJsonParser().parse(invalidMessage)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNotNull()
    }

    @Test
    fun `falls back to multi partner if single partner images missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("images")
        val message = PaymentMethodMessageJsonParser().parse(invalidMessage)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNotNull()
    }

    @Test
    fun `falls back to multi partner if single partner inline promotion missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("inline_partner_promotion")
        val message = PaymentMethodMessageJsonParser().parse(invalidMessage)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNotNull()
    }

    @Test
    fun `returns null if single partner images missing and multi partner invalid`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("images")

        invalidMessage
            .getJSONObject("content")
            .remove("promotion")
        val message = PaymentMethodMessageJsonParser().parse(invalidMessage)
        assertThat(message?.singlePartner).isNull()
        assertThat(message?.multiPartner).isNull()
    }

    private fun JSONObject.deepCopy() = JSONObject(this.toString())
}
