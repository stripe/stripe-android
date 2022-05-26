package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.stripe.android.core.injection.DUMMY_INJECTOR_KEY
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.SetupIntentClientSecret
import com.stripe.android.ui.core.forms.resources.LpmRepository.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.FormFragmentArguments
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.NameSpec
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
                fontResId = 0,
            ),
            primaryButton = PaymentSheet.PrimaryButton(
                colorsLight = PaymentSheet.PrimaryButtonColors.defaultLight.copy(background = 0),
                colorsDark = PaymentSheet.PrimaryButtonColors.defaultLight.copy(background = 0),
                shape = PaymentSheet.PrimaryButtonShape(
                    cornerRadiusDp = 0.0f,
                    borderStrokeWidthDp = 20.0f,
                ),
                typography = PaymentSheet.PrimaryButtonTypography(
                    fontResId = 0
                )
            )
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
            SupportedPaymentMethod(
                "bancontact",
                true,
                R.string.stripe_paymentsheet_payment_method_bancontact,
                R.drawable.stripe_ic_paymentsheet_pm_bancontact,
                PaymentMethodRequirements(emptySet(), null, false),
                LayoutSpec(listOf(NameSpec()))
            ),
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
