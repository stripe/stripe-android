package com.stripe.android.paymentsheet

import android.content.res.ColorStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.link.LinkAccountUpdate
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.WalletType
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures.BILLING_DETAILS
import com.stripe.android.model.StripeIntent
import com.stripe.android.paymentelement.ExperimentalCustomPaymentMethodsApi
import com.stripe.android.paymentsheet.PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode
import com.stripe.android.paymentsheet.model.PaymentIntentClientSecret
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.LinkState
import com.stripe.android.paymentsheet.state.PaymentElementLoader
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.paymentsheet.ui.BillingDetailsFormState
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.forms.FormFieldEntry
import org.mockito.kotlin.mock

internal object PaymentSheetFixtures {
    internal val STATUS_BAR_COLOR
        get() = "#121212".toColorInt()

    internal const val MERCHANT_DISPLAY_NAME = "Merchant, Inc."
    internal const val CLIENT_SECRET = "pi_1234_secret_1234"
    internal const val DIFFERENT_CLIENT_SECRET = "pi_4321_secret_4321"
    internal const val SETUP_CLIENT_SECRET = "seti_1234_secret_4321"
    internal const val PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER = "PaymentSheetTestIdentifier"
    internal const val FLOW_CONTROLLER_CALLBACK_TEST_IDENTIFIER = "FlowControllerTestIdentifier"

    internal val PAYMENT_INTENT_CLIENT_SECRET = PaymentIntentClientSecret(CLIENT_SECRET)
    internal val SETUP_INTENT_CLIENT_SECRET = PaymentIntentClientSecret(SETUP_CLIENT_SECRET)

    internal val INITIALIZATION_MODE_PAYMENT_INTENT = PaymentElementLoader.InitializationMode.PaymentIntent(
        clientSecret = CLIENT_SECRET
    )

    internal val CONFIG_MINIMUM = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    internal val CONFIG_WITH_EVERYTHING = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = PaymentSheet.CustomerConfiguration(
            "customer_id",
            "ek_123"
        ),
        googlePay = ConfigFixtures.GOOGLE_PAY,
        primaryButtonColor = ColorStateList.valueOf(Color.Black.toArgb()),
        defaultBillingDetails = PaymentSheet.BillingDetails(name = "Skyler"),
        allowsDelayedPaymentMethods = true,
        allowsPaymentMethodsRequiringShippingAddress = true,
        allowsRemovalOfLastSavedPaymentMethod = false,
        paymentMethodOrder = listOf("klarna", "afterpay", "card"),
        appearance = PaymentSheet.Appearance(
            colorsLight = PaymentSheet.Colors.configureDefaultLight(primary = Color(0)),
            colorsDark = PaymentSheet.Colors.configureDefaultDark(primary = Color(0)),
            shapes = PaymentSheet.Shapes(
                cornerRadiusDp = 0.0f,
                borderStrokeWidthDp = 0.0f
            ),
            typography = PaymentSheet.Typography(
                sizeScaleFactor = 1.1f,
                fontResId = 0
            ),
            primaryButton = PaymentSheet.PrimaryButton(
                colorsLight = PaymentSheet.PrimaryButtonColors(background = 0, onBackground = 0, border = 0),
                colorsDark = PaymentSheet.PrimaryButtonColors(background = 0, onBackground = 0, border = 0),
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
            address = AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )
    )

    const val DEFAULT_EPHEMERAL_KEY = "ek_6bpdbs8volf6ods1y6tf8oy9p9g64ehr"

    private val defaultCustomerConfig = PaymentSheet.CustomerConfiguration(
        id = "customer_id",
        ephemeralKeySecret = DEFAULT_EPHEMERAL_KEY
    )

    internal val CONFIG_CUSTOMER = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        customer = defaultCustomerConfig,
        paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
    )

    internal val EMPTY_CUSTOMER_STATE = CustomerState(
        id = defaultCustomerConfig.id,
        ephemeralKeySecret = defaultCustomerConfig.ephemeralKeySecret,
        customerSessionClientSecret = null,
        paymentMethods = listOf(),
        defaultPaymentMethodId = null,
    )

    internal val CONFIG_GOOGLEPAY
        get() = PaymentSheet.Configuration(
            merchantDisplayName = MERCHANT_DISPLAY_NAME,
            googlePay = ConfigFixtures.GOOGLE_PAY,
            paymentMethodLayout = PaymentSheet.PaymentMethodLayout.Horizontal,
        )

    internal val CONFIG_CUSTOMER_WITH_GOOGLEPAY
        get() = CONFIG_CUSTOMER.newBuilder()
            .googlePay(ConfigFixtures.GOOGLE_PAY)
            .build()

    internal val CONFIG_CUSTOMER_WITH_EXTERNAL_PAYMENT_METHODS
        get() = CONFIG_CUSTOMER.newBuilder()
            .externalPaymentMethods(listOf("external_paypal", "external_fawry"))
            .build()

    @OptIn(ExperimentalCustomPaymentMethodsApi::class)
    internal val CONFIG_CUSTOMER_WITH_CUSTOM_PAYMENT_METHODS
        get() = CONFIG_CUSTOMER.newBuilder()
            .customPaymentMethods(
                listOf(
                    PaymentSheet.CustomPaymentMethod(
                        id = "cpmt_123",
                        subtitle = "Pay now with BuFoPay".resolvableString,
                        disableBillingDetailCollection = false,
                    ),
                    PaymentSheet.CustomPaymentMethod(
                        id = "cpmt_456",
                        subtitle = "Pay now with PayPal".resolvableString,
                        disableBillingDetailCollection = true,
                    ),
                )
            )
            .build()

    internal val CONFIG_BILLING_DETAILS_COLLECTION = PaymentSheet.Configuration(
        merchantDisplayName = MERCHANT_DISPLAY_NAME,
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
            name = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            email = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            phone = PaymentSheet.BillingDetailsCollectionConfiguration.CollectionMode.Always,
            address = AddressCollectionMode.Full,
            attachDefaultsToPaymentMethod = true,
        )
    )

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
        paymentElementCallbackIdentifier = PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
        linkAccountInfo = LinkAccountUpdate.Value(null),
        walletsToShow = WalletType.entries,
    )

    internal fun PaymentOptionContract.Args.updateState(
        paymentMethods: List<PaymentMethod> = state.customer?.paymentMethods ?: emptyList(),
        isGooglePayReady: Boolean = state.paymentMethodMetadata.isGooglePayReady,
        stripeIntent: StripeIntent = state.stripeIntent,
        config: PaymentSheet.Configuration = configuration,
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

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY_SETUP
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.SetupIntent("seti_1234_secret_1234"),
            config = CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            paymentElementCallbackIdentifier = PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
            statusBarColor = STATUS_BAR_COLOR,
        )

    internal val ARGS_CUSTOMER_WITH_GOOGLEPAY
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            config = CONFIG_CUSTOMER_WITH_GOOGLEPAY,
            paymentElementCallbackIdentifier = PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
            statusBarColor = STATUS_BAR_COLOR
        )

    internal val ARGS_CUSTOMER_WITHOUT_GOOGLEPAY
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.PaymentIntent(
                clientSecret = PAYMENT_INTENT_CLIENT_SECRET.value,
            ),
            config = CONFIG_CUSTOMER,
            paymentElementCallbackIdentifier = PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
            statusBarColor = STATUS_BAR_COLOR
        )

    internal val ARGS_WITHOUT_CUSTOMER
        get() = ARGS_CUSTOMER_WITH_GOOGLEPAY.copy(
            config = ARGS_CUSTOMER_WITH_GOOGLEPAY.config.newBuilder()
                .customer(null)
                .build()
        )

    internal val ARGS_DEFERRED_INTENT
        get() = PaymentSheetContractV2.Args(
            initializationMode = PaymentElementLoader.InitializationMode.DeferredIntent(
                intentConfiguration = PaymentSheet.IntentConfiguration(
                    mode = PaymentSheet.IntentConfiguration.Mode.Payment(
                        amount = 10L,
                        currency = "USD"
                    )
                )
            ),
            config = CONFIG_CUSTOMER,
            paymentElementCallbackIdentifier = PAYMENT_SHEET_CALLBACK_TEST_IDENTIFIER,
            statusBarColor = STATUS_BAR_COLOR
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

    internal val BILLING_DETAILS_FORM_DETAILS = BILLING_DETAILS.copy(
        email = null,
        name = null,
        phone = null
    )

    internal fun billingDetailsFormState(
        line1: FormFieldEntry? = FormFieldEntry("1234 Main Street", isComplete = true),
        line2: FormFieldEntry? = null,
        city: FormFieldEntry? = FormFieldEntry("San Francisco", isComplete = true),
        postalCode: FormFieldEntry? = FormFieldEntry("94111", isComplete = true),
        state: FormFieldEntry? = FormFieldEntry("CA", isComplete = true),
        country: FormFieldEntry? = FormFieldEntry("US", isComplete = true),
        name: FormFieldEntry? = null,
        email: FormFieldEntry? = null,
        phone: FormFieldEntry? = null,
    ): BillingDetailsFormState {
        return BillingDetailsFormState(
            line1 = line1,
            line2 = line2,
            city = city,
            postalCode = postalCode,
            state = state,
            country = country,
            name = name,
            email = email,
            phone = phone,
        )
    }
}
