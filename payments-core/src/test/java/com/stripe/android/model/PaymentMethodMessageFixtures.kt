package com.stripe.android.model

internal object PaymentMethodMessageFixtures {
    val DEFAULT = """
        {
            "display_l_html":"\u003Cimg src=\"https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/klarna_logo_black.png\"\u003E\u003Cimg src=\"https://b.stripecdn.com/payment-method-messaging-statics-srv/assets/afterpay_logo_black.png\"\u003E\u003Cbr/\u003E4 interest-free payments of ${'$'}6.25.",
            "learn_more_modal_url":"js.stripe.com/v3/unified-message-redirect.html#componentName=unifiedMessage\u0026controllerId=__privateStripeController12345\u0026locale=en_US%2520%2528current%2529\u0026publicOptions%5Bamount%5D=2499\u0026publicOptions%5Bclient%5D=ios\u0026publicOptions%5BcountryCode%5D=US\u0026publicOptions%5Bcurrency%5D=USD\u0026publicOptions%5BpaymentMethods%5D%5B0%5D=afterpay_clearpay\u0026publicOptions%5BpaymentMethods%5D%5B1%5D=klarna"
        }
    """.trimIndent()
}
