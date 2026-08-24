package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val promptPayRawValues = mapOf(
    IdentifierSpec.Email to "pimchanok.sukjai@example.com",
    IdentifierSpec.Line1 to "1 ถนนสุขุมวิท",
    IdentifierSpec.Line2 to "ชั้น 2",
    IdentifierSpec.City to "กรุงเทพมหานคร",
    IdentifierSpec.PostalCode to "10110",
    IdentifierSpec.Country to "TH",
)

private val promptPayNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PromptPay.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.PromptPay.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val promptPayAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PromptPay.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "pimchanok.sukjai@example.com",
            address = Address(),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.PromptPay.code,
            "billing_details" to mapOf(
                "email" to "pimchanok.sukjai@example.com",
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val promptPayFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PromptPay.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "pimchanok.sukjai@example.com",
            address = Address(
                line1 = "1 ถนนสุขุมวิท",
                line2 = "ชั้น 2",
                city = "กรุงเทพมหานคร",
                state = "",
                country = "TH",
                postalCode = "10110",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.PromptPay.code,
            "billing_details" to mapOf(
                "email" to "pimchanok.sukjai@example.com",
                "address" to mapOf(
                    "line1" to "1 ถนนสุขุมวิท",
                    "line2" to "ชั้น 2",
                    "city" to "กรุงเทพมหานคร",
                    "state" to "",
                    "country" to "TH",
                    "postal_code" to "10110",
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

internal val promptPayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PromptPay Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PromptPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = promptPayRawValues,
        expectedParams = promptPayNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PromptPay Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PromptPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = promptPayRawValues,
        expectedParams = promptPayAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PromptPay Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PromptPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = promptPayRawValues,
        expectedParams = promptPayFullExpectedParams,
    ),
)
