package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val epsFullRawValues = mapOf(
    IdentifierSpec.Generic("eps[bank]") to "bank_austria",
    IdentifierSpec.Name to "Anna Gruber",
    IdentifierSpec.Line1 to "Kärntner Straße 1",
    IdentifierSpec.Line2 to "Top 2",
    IdentifierSpec.City to "Vienna",
    IdentifierSpec.PostalCode to "1010",
    IdentifierSpec.Country to "AT",
)

private val epsNoBillingDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Eps.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Eps.code,
        "eps" to mapOf("bank" to "bank_austria"),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val epsWithContactDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Eps.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Anna Gruber",
        address = Address(),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Eps.code,
        "eps" to mapOf("bank" to "bank_austria"),
        "billing_details" to mapOf(
            "name" to "Anna Gruber",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val epsWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Eps.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Anna Gruber",
        address = Address(
            line1 = "Kärntner Straße 1",
            line2 = "Top 2",
            city = "Vienna",
            country = "AT",
            postalCode = "1010",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Eps.code,
        "eps" to mapOf("bank" to "bank_austria"),
        "billing_details" to mapOf(
            "name" to "Anna Gruber",
            "address" to mapOf(
                "line1" to "Kärntner Straße 1",
                "line2" to "Top 2",
                "city" to "Vienna",
                "country" to "AT",
                "postal_code" to "1010",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val epsTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "EPS Never",
        paymentMethodType = PaymentMethod.Type.Eps,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = epsFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = epsNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "EPS Automatic without tax",
        paymentMethodType = PaymentMethod.Type.Eps,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = epsFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = epsWithContactDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "EPS Full",
        paymentMethodType = PaymentMethod.Type.Eps,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = epsFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = epsWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
