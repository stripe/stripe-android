package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val blikFullRawValues = mapOf(
    IdentifierSpec.BlikCode to "123456",
    IdentifierSpec.Line1 to "Marszalkowska 1",
    IdentifierSpec.Line2 to "Apartment 2",
    IdentifierSpec.City to "Warsaw",
    IdentifierSpec.PostalCode to "00-001",
    IdentifierSpec.Country to "PL",
)

private val blikExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Blik.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Blik.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val blikWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Blik.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "Marszalkowska 1",
            line2 = "Apartment 2",
            city = "Warsaw",
            country = "PL",
            postalCode = "00-001",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Blik.code,
        "billing_details" to mapOf(
            "address" to mapOf(
                "line1" to "Marszalkowska 1",
                "line2" to "Apartment 2",
                "city" to "Warsaw",
                "country" to "PL",
                "postal_code" to "00-001",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val blikTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Blik Never",
        paymentMethodType = PaymentMethod.Type.Blik,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = blikFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = blikExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.Blik("123456"),
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Blik Automatic without tax",
        paymentMethodType = PaymentMethod.Type.Blik,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = blikFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = blikExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.Blik("123456"),
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Blik Full",
        paymentMethodType = PaymentMethod.Type.Blik,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = blikFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = blikWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.Blik("123456"),
            extraParams = null,
        ),
    ),
)
