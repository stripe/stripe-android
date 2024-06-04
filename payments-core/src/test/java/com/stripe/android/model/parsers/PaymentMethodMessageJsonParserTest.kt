package com.stripe.android.model.parsers

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodMessageFixtures
import org.json.JSONObject
import org.junit.Test

class PaymentMethodMessageJsonParserTest {

    @Test
    fun parse_shouldCreateExpectedObject() {
        val paymentMethodMessage = PaymentMethodMessageJsonParser().parse(
            JSONObject(PaymentMethodMessageFixtures.DEFAULT)
        )
        assertThat(paymentMethodMessage?.displayHtml)
            .isEqualTo("<img src=\"https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/klarna_logo_black.png\"><img src=\"https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/afterpay_logo_black.png\"><br/>4 interest-free payments of \$6.25.")
        assertThat(paymentMethodMessage?.learnMoreUrl)
            .isEqualTo("js.stripe.com/v3/unified-message-redirect.html#componentName=unifiedMessage&controllerId=__privateStripeController12345&locale=en_US%2520%2528current%2529&publicOptions%5Bamount%5D=2499&publicOptions%5Bclient%5D=ios&publicOptions%5BcountryCode%5D=US&publicOptions%5Bcurrency%5D=USD&publicOptions%5BpaymentMethods%5D%5B0%5D=afterpay_clearpay&publicOptions%5BpaymentMethods%5D%5B1%5D=klarna")
    }

    @Test
    fun parseError_shouldCreatesEmptyObject() {
        val paymentMethodMessage = PaymentMethodMessageJsonParser().parse(
            JSONObject("{}")
        )

        assertThat(paymentMethodMessage)
            .isEqualTo(null)
    }
}
