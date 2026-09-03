package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val satispayRawValues = mapOf(
    IdentifierSpec.Line1 to "510 Townsend St",
    IdentifierSpec.Line2 to "Floor 2",
    IdentifierSpec.City to "San Francisco",
    IdentifierSpec.State to "CA",
    IdentifierSpec.PostalCode to "94103",
    IdentifierSpec.Country to "US",
)

private val satispayTypeOnlyExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Satispay.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Satispay.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val satispayAutomaticWithTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Satispay.code,
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
            "type" to PaymentMethod.Type.Satispay.code,
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

private val satispayFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Satispay.code,
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
            "type" to PaymentMethod.Type.Satispay.code,
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

private val satispayTypeOnlyWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Satispay.code,
        billingDetails = null,
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Satispay.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val satispayFullWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Satispay.code,
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
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Satispay.code,
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

internal val satispayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay Never PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay AutomaticWithoutTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay AutomaticWithTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayAutomaticWithTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay Full PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayFullExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay Never PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay AutomaticWithoutTax PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay Full PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay Never SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay AutomaticWithoutTax SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay Full SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay AutomaticWithoutTax PaymentIntentWithSetupFutureUsage never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Satispay AutomaticWithoutTax SetupIntent never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Satispay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = satispayRawValues,
        expectedParams = satispayTypeOnlyExpectedParams,
    ),
)
