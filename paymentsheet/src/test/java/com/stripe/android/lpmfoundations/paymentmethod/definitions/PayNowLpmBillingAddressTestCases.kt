package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val payNowRawValues = mapOf(
    IdentifierSpec.Line1 to "510 Townsend St",
    IdentifierSpec.Line2 to "Floor 2",
    IdentifierSpec.City to "San Francisco",
    IdentifierSpec.State to "CA",
    IdentifierSpec.PostalCode to "94103",
    IdentifierSpec.Country to "US",
)
private val payNowNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PayNow.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.PayNow.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val payNowAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PayNow.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.PayNow.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val payNowAutomaticWithTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PayNow.code,
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(
                line1 = "510 Townsend St",
                line2 = null,
                city = "San Francisco",
                state = "CA",
                country = "US",
                postalCode = "94103",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.PayNow.code,
            "billing_details" to mapOf(
                "address" to mapOf(
                    "line1" to "510 Townsend St",
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
    ),
    optionsParams = null,
    extraParams = null,
)

private val payNowFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.PayNow.code,
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
            "type" to PaymentMethod.Type.PayNow.code,
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
    ),
    optionsParams = null,
    extraParams = null,
)

internal val payNowTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PayNow Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PayNow,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = payNowRawValues,
        expectedParams = payNowNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PayNow Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PayNow,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = payNowRawValues,
        expectedParams = payNowAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PayNow AutomaticWithTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PayNow,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = payNowRawValues,
        expectedParams = payNowAutomaticWithTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "PayNow Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.PayNow,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = payNowRawValues,
        expectedParams = payNowFullExpectedParams,
    ),
)
