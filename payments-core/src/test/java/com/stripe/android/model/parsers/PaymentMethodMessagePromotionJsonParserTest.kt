package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessageFixtures
import org.json.JSONObject
import org.junit.Test

class PaymentMethodMessagePromotionJsonParserTest {
    @Test
    fun parsesNoContent() {
        val promotions = PaymentMethodMessagePromotionJsonParser()
            .parse(PaymentMethodMessageFixtures.NO_CONTENT_JSON)

        assertThat(promotions).isNotNull()
        assertThat(promotions.promotions).isEmpty()
    }

    @Test
    fun parsesSinglePromotion() {
        val promotions = PaymentMethodMessagePromotionJsonParser()
            .parse(PaymentMethodMessageFixtures.SINGLE_PARTNER_JSON)

        assertThat(promotions).isNotNull()
        assertThat(promotions.promotions.size).isEqualTo(1)
        assertThat(promotions.promotions[0].paymentMethodType).isEqualTo("Klarna")
        assertThat(promotions.promotions[0].message).isEqualTo("4 interest-free payments of \$25.00")
        assertThat(promotions.promotions[0].learnMore.message).isEqualTo("Learn more")
        assertThat(promotions.promotions[0].learnMore.url).isEqualTo(
            "https://b.stripecdn.com/payment-method-" +
            "messaging-statics-srv/assets/learn-more/index.html?amount=10000&country=US&currency=USD&key=" +
            "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFf" +
            "B7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=klarna"
        )
    }

    @Test
    fun parsesMultiplePromotions() {
        val promotions = PaymentMethodMessagePromotionJsonParser()
            .parse(PaymentMethodMessageFixtures.MULTI_PARTNER_JSON)

        assertThat(promotions).isNotNull()
        assertThat(promotions.promotions.size).isEqualTo(3)

        assertThat(promotions.promotions[0].paymentMethodType).isEqualTo("AFTERPAY_CLEARPAY")
        assertThat(promotions.promotions[0].message).isEqualTo("4 interest-free payments of \$22.50")
        assertThat(promotions.promotions[0].learnMore.message).isEqualTo("Learn more")
        assertThat(promotions.promotions[0].learnMore.url).isEqualTo(
            "https://b.stripecdn.com/payment-method-" +
            "messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=" +
            "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFf" +
            "B7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=afterpay_clearpay"
        )

        assertThat(promotions.promotions[1].paymentMethodType).isEqualTo("AFFIRM")
        assertThat(promotions.promotions[1].message).isEqualTo("4 interest-free payments of \$22.50")
        assertThat(promotions.promotions[1].learnMore.message).isEqualTo("Learn more")
        assertThat(promotions.promotions[1].learnMore.url).isEqualTo(
            "https://b.stripecdn.com/payment-method-" +
            "messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=" +
            "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFf" +
            "B7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=affirm"
        )

        assertThat(promotions.promotions[2].paymentMethodType).isEqualTo("KLARNA")
        assertThat(promotions.promotions[2].message).isEqualTo("4 interest-free payments of \$22.50")
        assertThat(promotions.promotions[2].learnMore.message).isEqualTo("Learn more")
        assertThat(promotions.promotions[2].learnMore.url).isEqualTo(
            "https://b.stripecdn.com/payment-method-" +
            "messaging-statics-srv/assets/learn-more/index.html?amount=9000&country=US&currency=USD&key=" +
            "pk_test_51HvTI7Lu5o3P18Zp6t5AgBSkMvWoTtA0nyA7pVYDqpfLkRtWun7qZTYCOHCReprfLM464yaBeF72UFf" +
            "B7cY9WG4a00ZnDtiC2C&locale=en&payment_methods%5B0%5D=klarna"
        )
    }

    @Test
    fun handlesInvalidJson() {
        val promotions = PaymentMethodMessagePromotionJsonParser()
            .parse(JSONObject("{asdf:sdaf}"))

        assertThat(promotions.promotions.isEmpty())
    }
}
