package com.stripe.android.paymentsheet

import androidx.core.graphics.toColorInt
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret

internal object PaymentSheetFixtures {
    internal val STATUS_BAR_COLOR = "#121212".toColorInt()

    internal const val MERCHANT_DISPLAY_NAME = "Widget Store"
    internal const val CLIENT_SECRET = "client_secret"

    internal val PAYMENT_INTENT_CLIENT_SECRET = PaymentIntentClientSecret(
        CLIENT_SECRET
    )

    internal val CONFIG_MINIMUM = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME
    )

    internal val CONFIG_CUSTOMER = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = PaymentSheet.CustomerConfiguration(
            "customer_id",
            "ephemeral_key"
        )
    )

    internal val CONFIG_GOOGLEPAY = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        googlePay = ConfigFixtures.GOOGLE_PAY
    )

    internal val CONFIG_CUSTOMER_WITH_GOOGLEPAY = CONFIG_CUSTOMER.copy(
        googlePay = ConfigFixtures.GOOGLE_PAY
    )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP = PaymentSheetContract.Args(
        SetupIntentClientSecret(CLIENT_SECRET),
        CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        STATUS_BAR_COLOR,
    )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetContract.Args(
        PAYMENT_INTENT_CLIENT_SECRET,
        CONFIG_CUSTOMER_WITH_GOOGLEPAY,
        STATUS_BAR_COLOR,
    )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY = PaymentSheetContract.Args(
        PAYMENT_INTENT_CLIENT_SECRET,
        CONFIG_CUSTOMER,
        STATUS_BAR_COLOR,
    )

    internal val ARGS_WITHOUT_CUSTOMER = PaymentSheetContract.Args(
        PAYMENT_INTENT_CLIENT_SECRET,
        config = null,
        STATUS_BAR_COLOR,
    )
}
