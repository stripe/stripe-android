package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val oxxoFullRawValues = mapOf(
    FormFieldId.Name to "Ana Garcia",
    FormFieldId.Email to "ana.garcia@example.com",
    FormFieldId.Line1 to "Paseo de la Reforma 222",
    FormFieldId.Line2 to "Piso 3",
    FormFieldId.City to "Ciudad de Mexico",
    FormFieldId.State to "CDMX",
    FormFieldId.PostalCode to "06600",
    FormFieldId.Country to "MX",
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

private val oxxoAutomaticWithTaxExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Oxxo.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Ana Garcia",
        email = "ana.garcia@example.com",
        address = Address(
            line2 = null,
            country = "MX",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Oxxo.code,
        "billing_details" to mapOf(
            "name" to "Ana Garcia",
            "email" to "ana.garcia@example.com",
            "address" to mapOf(
                "country" to "MX",
            ),
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
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Oxxo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = oxxoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = oxxoNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "OXXO Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Oxxo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = oxxoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = oxxoWithContactDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "OXXO AutomaticWithTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Oxxo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = oxxoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = oxxoAutomaticWithTaxExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "OXXO Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Oxxo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = oxxoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = oxxoWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
