package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessage
import com.stripe.android.model.PaymentMethodMessageFixtures
import org.json.JSONObject
import org.junit.Test

class PaymentMethodMessageJsonParserTest {

    @Test
    fun parsesNoContent() {
        val message = PaymentMethodMessageJsonParser()
            .parse(PaymentMethodMessageFixtures.NO_CONTENT_JSON) as PaymentMethodMessage.NoContent

        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).isEmpty()
    }

    @Test
    fun parsesSinglePartner() {
        val message = PaymentMethodMessageJsonParser()
            .parse(PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON) as PaymentMethodMessage.SinglePartner
        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).hasSize(1)
        assertThat(message.inlinePartnerPromotion).isNotNull()
        assertThat(message.learnMore).isNotNull()
        assertThat(message.darkImage).isNotNull()
        assertThat(message.lightImage).isNotNull()
        assertThat(message.flatImage).isNotNull()
    }

    @Test
    fun parsesMultiPartner() {
        val message = PaymentMethodMessageJsonParser()
            .parse(PaymentMethodMessageFixtures.MULTI_PARTNER_JSON) as PaymentMethodMessage.MultiPartner
        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).hasSize(3)
        assertThat(message.lightImages).hasSize(3)
        assertThat(message.darkImages).hasSize(3)
        assertThat(message.darkImages).hasSize(3)
        assertThat(message.learnMore).isNotNull()
    }

    @Test
    fun handlesInvalidJsonGracefullyAndReturnsUnexpectedError() {
        val message = PaymentMethodMessageJsonParser()
            .parse(JSONObject("{}")) as PaymentMethodMessage.UnexpectedError
        assertThat(message.message).isEqualTo("content not found")
    }

    @Test
    fun `returns UnexpectedError if top level content missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.MULTI_PARTNER_JSON.deepCopy()
        invalidMessage
            .remove("content")

        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.UnexpectedError
        assertThat(message.message).isEqualTo("content not found")
    }

    @Test
    fun `returns NoContent if learn more url present but empty multi partner`() {
        val invalidMessage = PaymentMethodMessageFixtures.MULTI_PARTNER_JSON.deepCopy()
        val learnMore = invalidMessage
            .getJSONObject("content")
            .getJSONObject("learn_more")

        learnMore.remove("url")
        learnMore.put("url", "")

        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.NoContent
        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).hasSize(3)
    }

    @Test
    fun `returns UnexpectedError if promotion missing multi partner`() {
        val invalidMessage = PaymentMethodMessageFixtures.MULTI_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONObject("content")
            .remove("promotion")
        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.UnexpectedError
        assertThat(message).isNotNull()
        assertThat(message.message).isEqualTo("content.promotion not found")
    }

    @Test
    fun `falls back to multi partner if single partner learn more missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("learn_more")
        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.MultiPartner
        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).hasSize(1)
    }

    @Test
    fun `falls back to multi partner if single partner images missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("images")
        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.MultiPartner
        assertThat(message).isNotNull()
        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).hasSize(1)
    }

    @Test
    fun `returns UnexpectedError if single partner content missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .remove("content")
        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.UnexpectedError
        assertThat(message).isNotNull()
        assertThat(message.message).isEqualTo(
            "payment_plan_groups.content not found"
        )
    }

    @Test
    fun `returns UnexpectedError if single partner inline promotion missing`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .remove("inline_partner_promotion")
        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.UnexpectedError
        assertThat(message).isNotNull()
        assertThat(message.message).isEqualTo(
            "payment_plan_groups.content.inline_partner_promotion not found"
        )
    }

    @Test
    fun `falls back to multi partner if single partner learn more url present but empty`() {
        val invalidMessage = PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON.deepCopy()
        val learnMore = invalidMessage
            .getJSONArray("payment_plan_groups")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONObject("learn_more")

        learnMore.remove("url")
        learnMore.put("url", "")

        val message = PaymentMethodMessageJsonParser()
            .parse(invalidMessage) as PaymentMethodMessage.MultiPartner
        assertThat(message).isNotNull()
        assertThat(message.paymentMethods).hasSize(1)
    }

    private fun JSONObject.deepCopy() = JSONObject(this.toString())
}
