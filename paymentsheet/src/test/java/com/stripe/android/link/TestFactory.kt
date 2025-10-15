package com.stripe.android.link

import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.financialconnections.model.FinancialConnectionsAccount
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConsentUi
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.ConsumerShippingAddress
import com.stripe.android.model.ConsumerShippingAddresses
import com.stripe.android.model.ConsumerSignUpConsentAction
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.EmailSource
import com.stripe.android.model.IncentiveEligibilitySession
import com.stripe.android.model.LinkAccountSession
import com.stripe.android.model.LinkMode
import com.stripe.android.model.MobileFallbackWebviewParams
import com.stripe.android.model.PassiveCaptchaParamsFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.SharePaymentDetails
import com.stripe.android.networking.RequestSurface
import com.stripe.android.payments.financialconnections.FinancialConnectionsAvailability
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetFixtures
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import org.mockito.kotlin.mock

internal object TestFactory {

    fun consumerSessionLookUp(
        publishableKey: String,
        exists: Boolean
    ): ConsumerSessionLookup {
        return CONSUMER_SESSION_LOOKUP.copy(
            publishableKey = publishableKey,
            exists = exists
        )
    }

    const val EMAIL = "email@stripe.com"
    const val CLIENT_SECRET = "client_secret"
    const val PUBLISHABLE_KEY = "publishable_key"
    const val MERCHANT_NAME = "merchantName"
    const val CUSTOMER_EMAIL = "customer@email.com"
    const val CUSTOMER_PHONE = "1234567890"
    const val CUSTOMER_BILLING_COUNTRY_CODE = "US"
    const val CUSTOMER_NAME = "Customer"
    const val AMOUNT = 100L
    const val CURRENCY = "USD"
    const val COUNTRY = "US"
    const val COUNTRY_INFERRING_METHOD = "PHONE_NUMBER"

    val VERIFIED_SESSION = ConsumerSession.VerificationSession(
        type = ConsumerSession.VerificationSession.SessionType.Sms,
        state = ConsumerSession.VerificationSession.SessionState.Verified
    )

    val VERIFICATION_STARTED_SESSION = ConsumerSession.VerificationSession(
        type = ConsumerSession.VerificationSession.SessionType.Sms,
        state = ConsumerSession.VerificationSession.SessionState.Started
    )

    val CONSUMER_SESSION = ConsumerSession(
        emailAddress = EMAIL,
        clientSecret = CLIENT_SECRET,
        verificationSessions = listOf(VERIFIED_SESSION),
        redactedPhoneNumber = "+1********42",
        redactedFormattedPhoneNumber = "+1 (***) ***-**42",
    )

    val CONSUMER_SESSION_LOOKUP = ConsumerSessionLookup(
        exists = true,
        consumerSession = CONSUMER_SESSION,
        errorMessage = null,
        publishableKey = PUBLISHABLE_KEY
    )

    val CONSUMER_SESSION_SIGN_UP = ConsumerSessionSignup(
        consumerSession = CONSUMER_SESSION,
        publishableKey = PUBLISHABLE_KEY
    )

    val MOBILE_FALLBACK_WEBVIEW_PARAMS = MobileFallbackWebviewParams(
        webViewRequirementType = MobileFallbackWebviewParams.WebviewRequirementType.Required,
        webviewOpenUrl = "https://fake_auth.stripe.com/mobile/12345"
    )

    val CONSUMER_SESSION_WITH_WEB_AUTH = CONSUMER_SESSION.copy(
        verificationSessions = listOf(),
        mobileFallbackWebviewParams = MOBILE_FALLBACK_WEBVIEW_PARAMS
    )

    val PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.createCard(
        CardParams(
            number = "4242424242424242",
            expMonth = 1,
            expYear = 27,
            cvc = "123",
        )
    )

    val CONSUMER_PAYMENT_DETAILS_CARD = ConsumerPaymentDetails.Card(
        id = "csmrpd_123",
        last4 = "4242",
        expiryYear = 2999,
        expiryMonth = 12,
        brand = CardBrand.Visa,
        cvcCheck = CvcCheck.Pass,
        isDefault = true,
        networks = emptyList(),
        funding = "CREDIT",
        nickname = null,
        billingAddress = ConsumerPaymentDetails.BillingAddress(
            name = null,
            line1 = null,
            line2 = null,
            locality = null,
            administrativeArea = null,
            countryCode = CountryCode.US,
            postalCode = "12312"
        )
    )

    val CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT = ConsumerPaymentDetails.BankAccount(
        id = "csmrpd_124",
        last4 = "4242",
        isDefault = false,
        bankName = "Stripe Test Bank",
        bankIconCode = null,
        nickname = null,
        billingAddress = null,
        billingEmailAddress = null,
    )

    val CONSUMER_PAYMENT_DETAILS_PASSTHROUGH = ConsumerPaymentDetails.Passthrough(
        id = "csmrpd_125",
        last4 = "4242",
        paymentMethodId = "pm_123"
    )

    val LINK_ACCOUNT_SESSION = LinkAccountSession(
        id = "fcsess_123",
        clientSecret = CLIENT_SECRET,
    )

    val LINK_NEW_PAYMENT_DETAILS = LinkPaymentDetails.New(
        paymentDetails = CONSUMER_PAYMENT_DETAILS_CARD,
        paymentMethodCreateParams = PAYMENT_METHOD_CREATE_PARAMS,
        originalParams = mock()
    )

    val LINK_SHARE_PAYMENT_DETAILS = SharePaymentDetails(
        paymentMethodId = "pm_123",
        encodedPaymentMethod = "{\"id\": \"pm_123\"}",
    )

    val LINK_SAVED_PAYMENT_DETAILS = LinkPaymentDetails.Saved(
        paymentDetails = CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        paymentMethodCreateParams = PAYMENT_METHOD_CREATE_PARAMS,
    )

    val LINK_ACCOUNT = LinkAccount(CONSUMER_SESSION)

    val LINK_ACCOUNT_WITH_PK = LinkAccount(CONSUMER_SESSION, PUBLISHABLE_KEY)

    val CONSUMER_PAYMENT_DETAILS: ConsumerPaymentDetails = ConsumerPaymentDetails(
        paymentDetails = listOf(
            CONSUMER_PAYMENT_DETAILS_CARD,
            CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        )
    )

    val CONSUMER_SHIPPING_ADDRESSES: ConsumerShippingAddresses = ConsumerShippingAddresses(
        addresses = listOf(
            ConsumerShippingAddress(
                id = "adr_123",
                isDefault = true,
                address = ConsumerPaymentDetails.BillingAddress(
                    name = "John Doe",
                    line1 = "123 Main St",
                    line2 = "Apt 4B",
                    locality = "San Francisco",
                    administrativeArea = "CA",
                    postalCode = "94105",
                    countryCode = CountryCode.US,
                ),
            )
        )
    )

    val LINK_CUSTOMER_INFO = LinkConfiguration.CustomerInfo(
        name = CUSTOMER_NAME,
        email = CUSTOMER_EMAIL,
        phone = CUSTOMER_PHONE,
        billingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE
    )

    val LINK_EMPTY_CUSTOMER_INFO = LinkConfiguration.CustomerInfo(
        name = null,
        email = null,
        phone = null,
        billingCountryCode = null
    )

    val LINK_CONFIGURATION = LinkConfiguration(
        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        merchantName = MERCHANT_NAME,
        sellerBusinessName = null,
        merchantCountryCode = "",
        merchantLogoUrl = null,
        customerInfo = LINK_CUSTOMER_INFO,
        shippingDetails = null,
        flags = emptyMap(),
        cardBrandChoice = null,
        cardBrandFilter = DefaultCardBrandFilter,
        financialConnectionsAvailability = FinancialConnectionsAvailability.Full,
        passthroughModeEnabled = false,
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
        defaultBillingDetails = null,
        useAttestationEndpointsForLink = false,
        suppress2faModal = false,
        initializationMode = PaymentSheetFixtures.INITIALIZATION_MODE_PAYMENT_INTENT,
        elementsSessionId = "session_1234",
        linkMode = LinkMode.LinkPaymentMethod,
        allowDefaultOptIn = false,
        disableRuxInFlowController = false,
        collectMissingBillingDetailsForExistingPaymentMethods = true,
        allowUserEmailEdits = true,
        allowLogOut = true,
        enableDisplayableDefaultValuesInEce = false,
        skipWalletInFlowController = false,
        linkAppearance = null,
        linkSignUpOptInFeatureEnabled = false,
        linkSignUpOptInInitialValue = false,
        customerId = null,
        saveConsentBehavior = PaymentMethodSaveConsentBehavior.Disabled(null),
        forceSetupFutureUseBehaviorAndNewMandate = false,
        linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD"),
        clientAttributionMetadata = null,
    )

    val LINK_CONFIGURATION_WITH_INSTANT_DEBITS_ONBOARDING = LINK_CONFIGURATION.copy(
        linkSupportedPaymentMethodsOnboardingEnabled = listOf("CARD", "INSTANT_DEBITS"),
    )

    val LINK_WALLET_PRIMARY_BUTTON_LABEL = Amount(
        requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.amount),
        requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.currency)
    ).buildPayButtonLabel()

    val LINK_WALLET_SECONDARY_BUTTON_LABEL = resolvableString(R.string.stripe_wallet_pay_another_way)

    val CARD_FORM_ARGS = FormArguments(
        paymentMethodCode = PaymentMethod.Type.Card.code,
        cbcEligibility = CardBrandChoiceEligibility.Ineligible,
        merchantName = "Example, Inc.",
        amount = Amount(1000, "USD"),
        billingDetails = null,
        shippingDetails = null,
        billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(),
        hasIntentToSetup = false,
        paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
    )

    val CARD_FORM_ELEMENTS = CardDefinition.formElements()

    const val VERIFICATION_TOKEN = "12356edtyf6esrte6r6dtd67"
    val INCENTIVE_ELIGIBILITY_SESSION = IncentiveEligibilitySession.PaymentIntent("pi_12345")
    const val APP_ID = "com.stripe.app"
    const val SESSION_ID = "element_sessions_123"
    val EMAIL_SOURCE = EmailSource.CUSTOMER_OBJECT
    val CONSENT_ACTION = ConsumerSignUpConsentAction.Checkbox

    val NATIVE_LINK_ARGS = NativeLinkArgs(
        configuration = LINK_CONFIGURATION,
        requestSurface = RequestSurface.PaymentElement,
        publishableKey = "",
        stripeAccountId = "",
        linkExpressMode = LinkExpressMode.DISABLED,
        linkAccountInfo = LinkAccountUpdate.Value(TestFactory.LINK_ACCOUNT),
        paymentElementCallbackIdentifier = "LinkNativeTestIdentifier",
        launchMode = LinkLaunchMode.Full,
        passiveCaptchaParams = PassiveCaptchaParamsFactory.passiveCaptchaParams(),
        attestOnIntentConfirmation = false,
    )

    val FINANCIAL_CONNECTIONS_CHECKING_ACCOUNT = FinancialConnectionsAccount(
        id = "la_1KMGIuClCIKljWvsLzbigpVh",
        displayName = "My Checking",
        institutionName = "My Bank",
        last4 = "3456",
        category = FinancialConnectionsAccount.Category.CASH,
        created = 1643221992,
        livemode = true,
        permissions = listOf(FinancialConnectionsAccount.Permissions.PAYMENT_METHOD),
        status = FinancialConnectionsAccount.Status.ACTIVE,
        subcategory = FinancialConnectionsAccount.Subcategory.CHECKING,
        supportedPaymentMethodTypes = listOf(
            FinancialConnectionsAccount.SupportedPaymentMethodTypes.US_BANK_ACCOUNT,
            FinancialConnectionsAccount.SupportedPaymentMethodTypes.LINK
        )
    )

    val CONSENT_PANE = ConsentUi.ConsentPane(
        title = "Test Consent",
        scopesSection = ConsentUi.ConsentPane.ScopesSection(
            header = "Test Header",
            scopes = listOf(
                ConsentUi.ConsentPane.ScopesSection.Scope(
                    icon = ConsentUi.Icon(default = "test_icon"),
                    header = "Test Scope",
                    description = "Test scope description"
                )
            )
        ),
        disclaimer = "Test disclaimer",
        denyButtonLabel = "Deny",
        allowButtonLabel = "Allow"
    )
}
