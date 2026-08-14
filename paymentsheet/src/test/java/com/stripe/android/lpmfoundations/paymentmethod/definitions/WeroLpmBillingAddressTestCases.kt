package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val weroCountryOnlyRawValues = mapOf(
    IdentifierSpec.Country to "DE",
)

private val weroWithBillingAddressRawValues = weroCountryOnlyRawValues + mapOf(
    IdentifierSpec.Line1 to "Unter den Linden 1",
    IdentifierSpec.Line2 to "Wohnung 2",
    IdentifierSpec.City to "Berlin",
    IdentifierSpec.PostalCode to "10117",
)

private val weroCountryOnlyExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Wero.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(country = "DE"),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Wero.code,
        "billing_details" to mapOf(
            "address" to mapOf("country" to "DE"),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val weroWithBillingAddressExpectedPaymentParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Wero.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "Unter den Linden 1",
            line2 = "Wohnung 2",
            city = "Berlin",
            country = "DE",
            postalCode = "10117",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Wero.code,
        "billing_details" to mapOf(
            "address" to mapOf(
                "line1" to "Unter den Linden 1",
                "line2" to "Wohnung 2",
                "city" to "Berlin",
                "country" to "DE",
                "postal_code" to "10117",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val weroTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Wero Never",
        paymentMethodType = PaymentMethod.Type.Wero,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = weroCountryOnlyRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = weroCountryOnlyExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Wero Automatic without tax",
        paymentMethodType = PaymentMethod.Type.Wero,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = weroCountryOnlyRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = weroCountryOnlyExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Wero Full",
        paymentMethodType = PaymentMethod.Type.Wero,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = weroWithBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = weroWithBillingAddressExpectedPaymentParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
