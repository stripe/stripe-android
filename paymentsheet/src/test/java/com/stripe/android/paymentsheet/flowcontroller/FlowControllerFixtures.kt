package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentMethodLayout
import com.stripe.android.paymentsheet.ConfigFixtures
import com.stripe.android.paymentsheet.state.CustomerState

internal object FlowControllerFixtures {
    internal const val MERCHANT_DISPLAY_NAME = "Merchant, Inc."
    internal const val CLIENT_SECRET = "pi_1234_secret_1234"
    internal const val DIFFERENT_CLIENT_SECRET = "pi_4321_secret_4321"
    internal const val SETUP_CLIENT_SECRET = "seti_1234_secret_4321"
    internal const val FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER = "FlowControllerTestIdentifier"

    internal val CONFIG_MINIMUM = FlowController.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        paymentMethodLayout = PaymentMethodLayout.Horizontal,
    )

    const val DEFAULT_EPHEMERAL_KEY = "ek_6bpdbs8volf6ods1y6tf8oy9p9g64ehr"

    private val defaultCustomerConfig = CustomerConfiguration(
        id = "customer_id",
        ephemeralKeySecret = DEFAULT_EPHEMERAL_KEY
    )

    internal val CONFIG_CUSTOMER = FlowController.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = defaultCustomerConfig,
        paymentMethodLayout = PaymentMethodLayout.Horizontal,
    )

    internal val EMPTY_CUSTOMER_STATE = CustomerState(
        id = defaultCustomerConfig.id,
        ephemeralKeySecret = defaultCustomerConfig.ephemeralKeySecret,
        customerSessionClientSecret = null,
        paymentMethods = listOf(),
        defaultPaymentMethodId = null,
    )

    internal val CONFIG_CUSTOMER_WITH_GOOGLEPAY
        get() = CONFIG_CUSTOMER.newBuilder()
            .googlePay(ConfigFixtures.GOOGLE_PAY)
            .build()
}
