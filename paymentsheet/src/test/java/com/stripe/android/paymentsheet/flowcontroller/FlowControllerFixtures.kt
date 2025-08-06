package com.stripe.android.paymentsheet.flowcontroller

import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.elements.CustomerConfiguration
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentMethodLayout
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.ConfigFixtures
import com.stripe.android.paymentsheet.PaymentOptionContract
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import org.mockito.kotlin.mock

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

    internal val CONFIG_GOOGLEPAY
        get() = FlowController.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            googlePay = ConfigFixtures.GOOGLE_PAY,
            paymentMethodLayout = PaymentMethodLayout.Horizontal,
        )

    internal val CONFIG_CUSTOMER_WITH_GOOGLEPAY
        get() = CONFIG_CUSTOMER.newBuilder()
            .googlePay(ConfigFixtures.GOOGLE_PAY)
            .build()

    internal val PAYMENT_OPTIONS_CONTRACT_ARGS = PaymentOptionContract.Args(
        state = PaymentSheetState.Full(
            customer = EMPTY_CUSTOMER_STATE,
            config = CONFIG_GOOGLEPAY.asCommonConfiguration(),
            paymentSelection = null,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        ),
        configuration = CONFIG_GOOGLEPAY,
        enableLogging = false,
        productUsage = mock(),
        paymentElementCallbackIdentifier = FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER,
        linkAccountInfo = LinkAccountUpdate.Value(null),
        walletsToShow = WalletType.entries,
    )

    internal fun PaymentOptionContract.Args.updateState(
        paymentMethods: List<PaymentMethod> = state.customer?.paymentMethods ?: emptyList(),
        isGooglePayReady: Boolean = state.paymentMethodMetadata.isGooglePayReady,
        stripeIntent: StripeIntent = state.stripeIntent,
        config: FlowController.Configuration = configuration,
        paymentSelection: PaymentSelection? = state.paymentSelection,
        linkState: LinkState? = state.paymentMethodMetadata.linkState,
    ): PaymentOptionContract.Args {
        return copy(
            state = state.copy(
                customer = CustomerState(
                    id = config.customer?.id ?: "cus_1",
                    ephemeralKeySecret = config.customer?.ephemeralKeySecret ?: "client_secret",
                    customerSessionClientSecret = null,
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = null,
                ),
                config = config.asCommonConfiguration(),
                paymentSelection = paymentSelection,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = stripeIntent,
                    isGooglePayReady = isGooglePayReady,
                    linkState = linkState,
                ),
            ),
            configuration = config,
        )
    }
}
