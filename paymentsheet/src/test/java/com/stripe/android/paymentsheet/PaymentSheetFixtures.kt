package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import android.graphics.Color
import androidx.core.graphics.toColorInt
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.state.CustomerState
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
        allowsPaymentMethodsRequiringShippingAddress = true,
        allowsRemovalOfLastSavedPaymentMethod = false,
        paymentMethodOrder = listOf("klarna", "afterpay", "card"),
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

    private val defaultCustomerConfig = PaymentSheet.CustomerConfiguration(
        id = "customer_id",
        ephemeralKeySecret = "ephemeral_key"
    )

    internal val CONFIG_CUSTOMER = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = defaultCustomerConfig,
    )

    internal val EMPTY_CUSTOMER_STATE = CustomerState(
        id = defaultCustomerConfig.id,
        ephemeralKeySecret = defaultCustomerConfig.ephemeralKeySecret,
        customerSessionClientSecret = null,
        paymentMethods = listOf(),
        permissions = CustomerState.Permissions(
            canRemovePaymentMethods = true,
            canRemoveDuplicates = false,
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

    internal val CONFIG_CUSTOMER_WITH_EXTERNAL_PAYMENT_METHODS
        get() = CONFIG_CUSTOMER.copy(
            externalPaymentMethods = listOf("external_paypal", "external_fawry")
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
            customer = EMPTY_CUSTOMER_STATE,
            config = CONFIG_GOOGLEPAY,
            paymentSelection = null,
            linkState = null,
            validationError = null,
            paymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        ),
        statusBarColor = STATUS_BAR_COLOR,
        enableLogging = false,
        productUsage = mock()
    )

    internal fun PaymentOptionContract.Args.updateState(
        paymentMethods: List<PaymentMethod> = state.customer?.paymentMethods ?: emptyList(),
        isGooglePayReady: Boolean = state.paymentMethodMetadata.isGooglePayReady,
        stripeIntent: StripeIntent = state.stripeIntent,
        config: PaymentSheet.Configuration = state.config,
        paymentSelection: PaymentSelection? = state.paymentSelection,
        linkState: LinkState? = state.linkState,
    ): PaymentOptionContract.Args {
        return copy(
            state = state.copy(
                customer = CustomerState(
                    id = config.customer?.id ?: "cus_1",
                    ephemeralKeySecret = config.customer?.ephemeralKeySecret ?: "client_secret",
                    customerSessionClientSecret = null,
                    paymentMethods = paymentMethods,
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = true,
                        canRemoveDuplicates = false,
                    )
                ),
                config = config,
                paymentSelection = paymentSelection,
                linkState = linkState,
                paymentMethodMetadata = PaymentMethodMetadataFactory.create(
                    stripeIntent = stripeIntent,
                    isGooglePayReady = isGooglePayReady,
                ),
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

    internal val ARGS_DEFERRED_INTENT
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentSheet.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD"
                    )
                )
            ),
            CONFIG_CUSTOMER,
            STATUS_BAR_COLOR
        )

    internal val COMPOSE_FRAGMENT_ARGS
        get() = FormArguments(
            PaymentMethod.Type.Bancontact.code,
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
            cbcEligibility = CardBrandChoiceEligibility.Ineligible,
            hasIntentToSetup = false,
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
        )

    internal val PAYPAL_AND_VENMO_EXTERNAL_PAYMENT_METHOD_DATA = """
       [
            {
                "dark_image_url":null,
                "label":"Venmo",
                "light_image_url":"https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-epm-venmo-162b3cf0020c8fe2ce4bde7ec3845941.png",
                "type":"external_venmo"
            },
            {
                "dark_image_url":"https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-paypal_dark@3x-26040e151c8f87187da2f997791fcc31.png",
                "label":"PayPal",
                "light_image_url":"https:\/\/js.stripe.com\/v3\/fingerprinted\/img\/payment-methods\/icon-pm-paypal@3x-5227ab4fca3d36846bd6622f495cdf4b.png",
                "type":"external_paypal"
            }
        ] 
    """.trimIndent()
}
