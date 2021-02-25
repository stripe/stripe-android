package com.stripe.android.paymentsheet

import androidx.core.graphics.toColorInt
import com.stripe.android.paymentsheet.analytics.SessionId

internal object PaymentSheetFixtures {
    internal val STATUS_BAR_COLOR = "#121212".toColorInt()

    private const val MERCHANT_DISPLAY_NAME = "Widget Store"
    internal const val CLIENT_SECRET = "client_secret"

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

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY = PaymentSheetContract.Args(
        CLIENT_SECRET,
        SessionId(),
        STATUS_BAR_COLOR,
        CONFIG_CUSTOMER_WITH_GOOGLEPAY
    )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY = PaymentSheetContract.Args(
        CLIENT_SECRET,
        SessionId(),
        STATUS_BAR_COLOR,
        CONFIG_CUSTOMER
    )

    internal val ARGS_WITHOUT_CUSTOMER = PaymentSheetContract.Args(
        CLIENT_SECRET,
        SessionId(),
        STATUS_BAR_COLOR,
        config = null
    )
}
