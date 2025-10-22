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
        assertThat(message?.inlinePartnerPromotion).isNull()
        assertThat(message?.promotion).isNull()
        assertThat(message?.lightImages).isEmpty()
        assertThat(message?.darkImages).isEmpty()
        assertThat(message?.flatImages).isEmpty()
    }

    @Test
    fun parsesSinglePartner() {
        val message = PaymentMethodMessageJsonParser().parse(PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON)
        assertThat(message).isNotNull()
        assertThat(message?.paymentMethods).hasSize(1)
        assertThat(message?.inlinePartnerPromotion).isNotNull()
        assertThat(message?.promotion).isNotNull()
        assertThat(message?.lightImages).hasSize(1)
        assertThat(message?.darkImages).hasSize(1)
        assertThat(message?.flatImages).hasSize(1)
    }

    @Test
    fun parsesMultiPartner() {
        val message = PaymentMethodMessageJsonParser().parse(PaymentMethodMessageFixtures.MULTI_PARTNER_JSON)
        assertThat(message).isNotNull()
        assertThat(message?.paymentMethods).hasSize(3)
        assertThat(message?.inlinePartnerPromotion).isNull()
        assertThat(message?.promotion).isNotNull()
        assertThat(message?.lightImages).hasSize(3)
        assertThat(message?.darkImages).hasSize(3)
        assertThat(message?.flatImages).hasSize(3)
    }
}
