package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import org.mockito.kotlin.mock

internal object PaymentSheetFixtures {
    internal val STATUS_BAR_COLOR
        get() = "#121212".toColorInt()

    internal const val MERCHANT_DISPLAY_NAME = "Merchant, Inc."
    internal const val CLIENT_SECRET = "pi_1234_secret_1234"
    internal const val DIFFERENT_CLIENT_SECRET = "pi_4321_secret_4321"
    internal const val SETUP_CLIENT_SECRET = "seti_1234_secret_4321"

    internal val PAYMENT_INTENT_CLIENT_SECRET = PaymentIntentClientSecret(CLIENT_SECRET)
    internal val SETUP_INTENT_CLIENT_SECRET = PaymentIntentClientSecret(SETUP_CLIENT_SECRET)

    internal val CONFIG_MINIMUM = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME
    )

    internal val CONFIG_WITH_EVERYTHING = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = PaymentSheet.CustomerConfiguration(
            "customer_id",
            "ephemeral_key"
        ),
        googlePay = ConfigFixtures.GOOGLE_PAY,
        primaryButtonColor = ColorStateList.valueOf(Color.BLACK),
        defaultBillingDetails = PaymentSheet.BillingDetails(name = "Skyler"),
        allowsDelayedPaymentMethods = true,
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.defaultLight.copy(primary = 0),
            colorsDark = PaymentSheet.Colors.defaultDark.copy(primary = 0),
            shapes = PaymentSheet.Shapes(
                cornerRadiusDp = 0.0f,
                borderStrokeWidthDp = 0.0f
            ),
            typography = PaymentSheet.Typography.default.copy(
                sizeScaleFactor = 1.1f,
                fontResId = 0
            ),
            primaryButton = PaymentSheet.PrimaryButton(
                colorsLight = PaymentSheet.PrimaryButtonColors.defaultLight.copy(background = 0),
                colorsDark = PaymentSheet.PrimaryButtonColors.defaultLight.copy(background = 0),
                shape = PaymentSheet.PrimaryButtonShape(
                    cornerRadiusDp = 0.0f,
                    borderStrokeWidthDp = 20.0f
                ),
                typography = PaymentSheet.PrimaryButtonTypography(
                    fontResId = 0
                )
            )
        ),
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )
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

    internal val CONFIG_BILLING_DETAILS_COLLECTION = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )
    )

    internal val PAYMENT_OPTIONS_CONTRACT_ARGS = PaymentOptionContract.Args(
        state = PaymentSheetState.Full(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD,
            customerPaymentMethods = emptyList(),
            config = CONFIG_GOOGLEPAY,
            isGooglePayReady = false,
            paymentSelection = null,
            linkState = null,
            isEligibleForCardBrandChoice = false,
        ),
        statusBarColor = STATUS_BAR_COLOR,
        enableLogging = false,
        productUsage = mock()
    )

    internal fun PaymentOptionContract.Args.updateState(
        paymentMethods: List<PaymentMethod> = state.customerPaymentMethods,
        isGooglePayReady: Boolean = state.isGooglePayReady,
        stripeIntent: StripeIntent = state.stripeIntent,
        config: PaymentSheet.Configuration = state.config,
        paymentSelection: PaymentSelection? = state.paymentSelection,
        linkState: LinkState? = state.linkState,
    ): PaymentOptionContract.Args {
        return copy(
            state = state.copy(
                customerPaymentMethods = paymentMethods,
                isGooglePayReady = isGooglePayReady,
                stripeIntent = stripeIntent,
                config = config,
                paymentSelection = paymentSelection,
                linkState = linkState,
            ),
        )
    }

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.SetupIntent("seti_1234_secret_1234"),
            CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            STATUS_BAR_COLOR
        )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            STATUS_BAR_COLOR
        )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.PaymentIntent(
                clientSecret = PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            CONFIG_CUSTOMER,
            STATUS_BAR_COLOR
        )

    internal val ARGS_WITHOUT_CUSTOMER
        get() = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.copy(
                customer = null
            )
        )

    internal val COMPOSE_FRAGMENT_ARGS
        get() = FormArguments(
            PaymentMethod.Type.Bancontact.code,
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
                    country = "DE"
                ),
                email = "email",
                name = "Jenny Rosen",
                phone = "+18008675309"
            ),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )
}
