package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val zipRawValues = mapOf(
    IdentifierSpec.Line1 to "510 Townsend St",
    IdentifierSpec.Line2 to "Floor 2",
    IdentifierSpec.City to "San Francisco",
    IdentifierSpec.State to "CA",
    IdentifierSpec.PostalCode to "94103",
    IdentifierSpec.Country to "US",
)
private val zipNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Zip.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Zip.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val zipAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Zip.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Zip.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val zipFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Zip.code,
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
            "type" to PaymentMethod.Type.Zip.code,
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

internal val zipTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Zip Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Zip,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = zipRawValues,
        expectedParams = zipNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Zip Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Zip,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = zipRawValues,
        expectedParams = zipAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Zip Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Zip,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = zipRawValues,
        expectedParams = zipFullExpectedParams,
    ),
)
