package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val naverPayRawValues = mapOf(
    FormFieldId.Generic("naver_pay[funding]") to "card",
    FormFieldId.Line1 to "510 Townsend St",
    FormFieldId.Line2 to "Floor 2",
    FormFieldId.City to "San Francisco",
    FormFieldId.State to "CA",
    FormFieldId.PostalCode to "94103",
    FormFieldId.Country to "US",
)

private val naverPayExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.NaverPay.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.NaverPay.code,
        "naver_pay" to mapOf(
            "funding" to "card",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val naverPayWithBillingAddressExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.NaverPay.code,
    billingDetails = PaymentMethod.BillingDetails(
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
        "type" to PaymentMethod.Type.NaverPay.code,
        "naver_pay" to mapOf(
            "funding" to "card",
        ),
        "billing_details" to mapOf(
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

internal val naverPayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Naver Pay Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.NaverPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = naverPayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = naverPayExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Naver Pay Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.NaverPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = naverPayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = naverPayExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Naver Pay Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.NaverPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = naverPayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = naverPayWithBillingAddressExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
