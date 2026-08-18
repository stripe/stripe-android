package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val mobilePayFullRawValues = mapOf(
    IdentifierSpec.Line1 to "Kongens Nytorv 1",
    IdentifierSpec.Line2 to "2. sal",
    IdentifierSpec.City to "København",
    IdentifierSpec.PostalCode to "1050",
    IdentifierSpec.Country to "DK",
)

private val mobilePayNoBillingDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.MobilePay.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.MobilePay.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val mobilePayWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.MobilePay.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "Kongens Nytorv 1",
            line2 = "2. sal",
            city = "København",
            country = "DK",
            postalCode = "1050",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.MobilePay.code,
        "billing_details" to mapOf(
            "address" to mapOf(
                "line1" to "Kongens Nytorv 1",
                "line2" to "2. sal",
                "city" to "København",
                "country" to "DK",
                "postal_code" to "1050",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val mobilePayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MobilePay Never",
        paymentMethodType = PaymentMethod.Type.MobilePay,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = mobilePayFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = mobilePayNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MobilePay Automatic without tax",
        paymentMethodType = PaymentMethod.Type.MobilePay,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = mobilePayFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = mobilePayNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MobilePay Full",
        paymentMethodType = PaymentMethod.Type.MobilePay,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = mobilePayFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = mobilePayWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
