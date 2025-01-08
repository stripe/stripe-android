package com.stripe.android.link

import com.stripe.android.core.model.CountryCode
import com.stripe.android.link.model.LinkAccount
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.lpmfoundations.paymentmethod.definitions.CardDefinition
import com.stripe.android.lpmfoundations.paymentmethod.formElements
import com.stripe.android.model.CardBrand
import com.stripe.android.model.CardParams
import com.stripe.android.model.ConsumerPaymentDetails
import com.stripe.android.model.ConsumerSession
import com.stripe.android.model.ConsumerSessionLookup
import com.stripe.android.model.ConsumerSessionSignup
import com.stripe.android.model.CvcCheck
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
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

    val VERIFIED_SESSION = ConsumerSession.VerificationSession(
        type = ConsumerSession.VerificationSession.SessionType.Sms,
        state = ConsumerSession.VerificationSession.SessionState.Verified
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

    val PAYMENT_METHOD_CREATE_PARAMS = PaymentMethodCreateParams.createCard(
        CardParams(
            number = "4242424242424242",
            expMonth = 1,
            expYear = 27,
            cvc = "123",
        )
    )

    val CONSUMER_PAYMENT_DETAILS_CARD = ConsumerPaymentDetails.Card(
        id = "pm_123",
        last4 = "4242",
        expiryYear = 2999,
        expiryMonth = 12,
        brand = CardBrand.Visa,
        cvcCheck = CvcCheck.Pass,
        isDefault = true,
        billingAddress = ConsumerPaymentDetails.BillingAddress(
            countryCode = CountryCode.US,
            postalCode = "12312"
        )
    )

    val CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT = ConsumerPaymentDetails.BankAccount(
        id = "pm_124",
        last4 = "4242",
        isDefault = false,
        bankName = "Stripe Test Bank",
        bankIconCode = null
    )

    val CONSUMER_PAYMENT_DETAILS_PASSTHROUGH = ConsumerPaymentDetails.Passthrough(
        id = "pm_125",
        last4 = "4242",
    )

    val LINK_NEW_PAYMENT_DETAILS = LinkPaymentDetails.New(
        paymentDetails = CONSUMER_PAYMENT_DETAILS_CARD,
        paymentMethodCreateParams = PAYMENT_METHOD_CREATE_PARAMS,
        originalParams = mock()
    )

    val LINK_ACCOUNT = LinkAccount(CONSUMER_SESSION)

    val CONSUMER_PAYMENT_DETAILS: ConsumerPaymentDetails = ConsumerPaymentDetails(
        paymentDetails = listOf(
            CONSUMER_PAYMENT_DETAILS_CARD,
            CONSUMER_PAYMENT_DETAILS_BANK_ACCOUNT,
            CONSUMER_PAYMENT_DETAILS_PASSTHROUGH,
        )
    )

    val LINK_CONFIGURATION = LinkConfiguration(
        stripeIntent = PaymentIntentFixtures.PI_SUCCEEDED,
        merchantName = MERCHANT_NAME,
        merchantCountryCode = "",
        customerInfo = LinkConfiguration.CustomerInfo(
            name = CUSTOMER_NAME,
            email = CUSTOMER_EMAIL,
            phone = CUSTOMER_PHONE,
            billingCountryCode = CUSTOMER_BILLING_COUNTRY_CODE
        ),
        shippingDetails = null,
        flags = emptyMap(),
        cardBrandChoice = null,
        passthroughModeEnabled = false
    )

    val LINK_WALLET_PRIMARY_BUTTON_LABEL = Amount(
        requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.amount),
        requireNotNull(PaymentIntentFixtures.PI_SUCCEEDED.currency)
    ).buildPayButtonLabel()

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
}
