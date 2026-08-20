package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val afterpayClearpayRawValues = mapOf(
    IdentifierSpec.Email to "jenny.rosen@example.com",
    IdentifierSpec.Line1 to "510 Townsend St",
    IdentifierSpec.Line2 to "Floor 2",
    IdentifierSpec.City to "San Francisco",
    IdentifierSpec.State to "CA",
    IdentifierSpec.PostalCode to "94103",
    IdentifierSpec.Country to "US",
)

private val afterpayClearpayNoBillingDetailsExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.AfterpayClearpay.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.AfterpayClearpay.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val afterpayClearpayWithEmailExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.AfterpayClearpay.code,
    billingDetails = PaymentMethod.BillingDetails(
        email = "jenny.rosen@example.com",
        address = Address(),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.AfterpayClearpay.code,
        "billing_details" to mapOf(
            "email" to "jenny.rosen@example.com",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val afterpayClearpayWithBillingAddressExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.AfterpayClearpay.code,
    billingDetails = PaymentMethod.BillingDetails(
        email = "jenny.rosen@example.com",
        address = Address(
            line1 = "510 Townsend St",
            line2 = "Floor 2",
            city = "San Francisco",
            state = "CA",
            country = "US",
            postalCode = "94103",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.AfterpayClearpay.code,
        "billing_details" to mapOf(
            "email" to "jenny.rosen@example.com",
            "address" to mapOf(
                "line1" to "510 Townsend St",
                "line2" to "Floor 2",
                "city" to "San Francisco",
                "state" to "CA",
                "country" to "US",
                "postal_code" to "94103",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val afterpayClearpayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Afterpay/Clearpay Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AfterpayClearpay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = afterpayClearpayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = afterpayClearpayNoBillingDetailsExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Afterpay/Clearpay Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AfterpayClearpay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = afterpayClearpayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = afterpayClearpayWithEmailExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Afterpay/Clearpay Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AfterpayClearpay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = afterpayClearpayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = afterpayClearpayWithBillingAddressExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
