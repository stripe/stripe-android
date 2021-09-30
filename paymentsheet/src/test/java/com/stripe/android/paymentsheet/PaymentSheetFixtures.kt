package com.stripe.android.paymentsheet

import androidx.core.graphics.toColorInt
import com.stripe.android.payments.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.paymentsheet.elements.Requirement
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments

internal object PaymentSheetFixtures {
    internal val STATUS_BAR_COLOR
        get() = "#121212".toColorInt()

    internal const val MERCHANT_DISPLAY_NAME = "Merchant, Inc."
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

    internal val CONFIG_GOOGLEPAY
        get() = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            googlePay = ConfigFixtures.GOOGLE_PAY
        )

    internal val CONFIG_CUSTOMER_WITH_GOOGLEPAY
        get() = CONFIG_CUSTOMER.copy(
            googlePay = ConfigFixtures.GOOGLE_PAY
        )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        get() = PaymentSheetContract.Args(
            SetupIntentClientSecret(CLIENT_SECRET),
            CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            STATUS_BAR_COLOR,
        )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY
        get() = PaymentSheetContract.Args(
            PAYMENT_INTENT_CLIENT_SECRET,
            CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            STATUS_BAR_COLOR,
        )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY
        get() = PaymentSheetContract.Args(
            PAYMENT_INTENT_CLIENT_SECRET,
            CONFIG_CUSTOMER,
            STATUS_BAR_COLOR,
        )

    internal val ARGS_WITHOUT_CUSTOMER
        get() = PaymentSheetContract.Args(
            PAYMENT_INTENT_CLIENT_SECRET,
            config = null,
            STATUS_BAR_COLOR,
        )

    internal val COMPOSE_FRAGMENT_ARGS
        get() = FormFragmentArguments(
            SupportedPaymentMethod.Bancontact,
            capabilities = setOf(
                Requirement.OneTimeUse,
                Requirement.DelayedSettlementSupport,
                Requirement.ShippingInIntentAddressCountry,
                Requirement.ShippingInIntentAddressLine1,
                Requirement.ShippingInIntentName
            ),
            merchantName = "Merchant, Inc.",
            billingDetails = PaymentSheet.BillingDetails(
                address = PaymentSheet.Address(
                    line1 = "123 Main Street",
                    line2 = null,
                    city = "San Francisco",
                    state = "CA",
                    postalCode = "94111",
                    country = "DE",
                ),
                email = "email",
                name = "Jenny Rosen",
                phone = "+18008675309"
            ),
            injectorKey = DUMMY_INJECTOR_KEY
        )
}
