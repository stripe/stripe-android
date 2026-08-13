package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val oxxoFullRawValues = mapOf(
    IdentifierSpec.Name to "Ana Garcia",
    IdentifierSpec.Email to "ana.garcia@example.com",
    IdentifierSpec.Line1 to "Paseo de la Reforma 222",
    IdentifierSpec.Line2 to "Piso 3",
    IdentifierSpec.City to "Ciudad de Mexico",
    IdentifierSpec.State to "CDMX",
    IdentifierSpec.PostalCode to "06600",
    IdentifierSpec.Country to "MX",
)

private val oxxoNoBillingDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Oxxo.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Oxxo.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val oxxoWithContactDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Oxxo.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Ana Garcia",
        email = "ana.garcia@example.com",
        address = Address(),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Oxxo.code,
        "billing_details" to mapOf(
            "name" to "Ana Garcia",
            "email" to "ana.garcia@example.com",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val oxxoWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Oxxo.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Ana Garcia",
        email = "ana.garcia@example.com",
        address = Address(
            line1 = "Paseo de la Reforma 222",
            line2 = "Piso 3",
            city = "Ciudad de Mexico",
            state = "CDMX",
            country = "MX",
            postalCode = "06600",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Oxxo.code,
        "billing_details" to mapOf(
            "name" to "Ana Garcia",
            "email" to "ana.garcia@example.com",
            "address" to mapOf(
                "line1" to "Paseo de la Reforma 222",
                "line2" to "Piso 3",
                "city" to "Ciudad de Mexico",
                "state" to "CDMX",
                "country" to "MX",
                "postal_code" to "06600",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val oxxoTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "OXXO Never",
        paymentMethodType = PaymentMethod.Type.Oxxo,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = oxxoFullRawValues,
        expectedPaymentMethodParams = oxxoNoBillingDetailsExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "OXXO Automatic without tax",
        paymentMethodType = PaymentMethod.Type.Oxxo,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = oxxoFullRawValues,
        expectedPaymentMethodParams = oxxoWithContactDetailsExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "OXXO Full",
        paymentMethodType = PaymentMethod.Type.Oxxo,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = oxxoFullRawValues,
        expectedPaymentMethodParams = oxxoWithBillingAddressExpectedPaymentMethodParams,
    ),
)
