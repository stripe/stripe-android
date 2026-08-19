package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val multibancoRawValues = mapOf(
    IdentifierSpec.Email to "ines.silva@example.com",
    IdentifierSpec.Line1 to "Rua do Ouro 1",
    IdentifierSpec.Line2 to "2.º Esq.",
    IdentifierSpec.City to "Lisboa",
    IdentifierSpec.PostalCode to "1100-063",
    IdentifierSpec.Country to "PT",
)

private val multibancoNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Multibanco.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Multibanco.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val multibancoAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Multibanco.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "ines.silva@example.com",
            address = Address(),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Multibanco.code,
            "billing_details" to mapOf(
                "email" to "ines.silva@example.com",
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val multibancoFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Multibanco.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "ines.silva@example.com",
            address = Address(
                line1 = "Rua do Ouro 1",
                line2 = "2.º Esq.",
                city = "Lisboa",
                country = "PT",
                postalCode = "1100-063",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Multibanco.code,
            "billing_details" to mapOf(
                "email" to "ines.silva@example.com",
                "address" to mapOf(
                    "line1" to "Rua do Ouro 1",
                    "line2" to "2.º Esq.",
                    "city" to "Lisboa",
                    "country" to "PT",
                    "postal_code" to "1100-063",
                ),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

internal val multibancoTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Multibanco Never",
        paymentMethodType = PaymentMethod.Type.Multibanco,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = multibancoRawValues,
        expectedParams = multibancoNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Multibanco Automatic without tax",
        paymentMethodType = PaymentMethod.Type.Multibanco,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = multibancoRawValues,
        expectedParams = multibancoAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Multibanco Full",
        paymentMethodType = PaymentMethod.Type.Multibanco,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = multibancoRawValues,
        expectedParams = multibancoFullExpectedParams,
    ),
)
