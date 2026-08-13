package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val klarnaFullRawValues = mapOf(
    IdentifierSpec.Country to "DE",
    IdentifierSpec.Email to "jane@example.com",
    IdentifierSpec.Line1 to "Unter den Linden 1",
    IdentifierSpec.Line2 to "Wohnung 2",
    IdentifierSpec.City to "Berlin",
    IdentifierSpec.PostalCode to "10117",
)

private val klarnaCountryOnlyExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Klarna.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(country = "DE"),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Klarna.code,
        "billing_details" to mapOf(
            "address" to mapOf("country" to "DE"),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val klarnaWithEmailAndCountryExpectedPaymentMethodParams =
    PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "jane@example.com",
            address = Address(country = "DE"),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "email" to "jane@example.com",
                "address" to mapOf("country" to "DE"),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    )

private val klarnaWithBillingAddressExpectedPaymentMethodParams =
    PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "jane@example.com",
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
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "email" to "jane@example.com",
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

internal val klarnaTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Never",
        paymentMethodType = PaymentMethod.Type.Klarna,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = klarnaFullRawValues,
        expectedPaymentMethodParams = klarnaCountryOnlyExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Automatic without tax",
        paymentMethodType = PaymentMethod.Type.Klarna,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = klarnaFullRawValues,
        expectedPaymentMethodParams = klarnaWithEmailAndCountryExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Full",
        paymentMethodType = PaymentMethod.Type.Klarna,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = klarnaFullRawValues,
        expectedPaymentMethodParams = klarnaWithBillingAddressExpectedPaymentMethodParams,
    ),
)
