package com.stripe.android.paymentsheet

import androidx.core.graphics.toColorInt
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import org.mockito.kotlin.mock

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

    internal val PAYMENT_OPTIONS_CONTRACT_ARGS = PaymentOptionContract.Args(
        stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
        paymentMethods = emptyList(),
        config = CONFIG_GOOGLEPAY,
        isGooglePayReady = false,
        newLpm = null,
        statusBarColor = STATUS_BAR_COLOR,
        injectorKey = DUMMY_INJECTOR_KEY,
        enableLogging = false,
        productUsage = mock()
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

    internal val ARGS_WITHOUT_CONFIG
        get() = PaymentSheetContract.Args(
            PAYMENT_INTENT_CLIENT_SECRET,
            config = null,
            STATUS_BAR_COLOR,
        )

    internal val ARGS_WITHOUT_CUSTOMER
        get() = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config?.copy(
                customer = null
            )
        )

    internal val COMPOSE_FRAGMENT_ARGS
        get() = FormFragmentArguments(
            SupportedPaymentMethod.Bancontact,
            showCheckbox = true,
            showCheckboxControlledFields = true,
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
