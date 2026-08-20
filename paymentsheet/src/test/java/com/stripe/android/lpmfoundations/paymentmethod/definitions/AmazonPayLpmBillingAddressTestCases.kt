package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val amazonPayRawValues = mapOf(
    IdentifierSpec.Line1 to "510 Townsend St",
    IdentifierSpec.Line2 to "Floor 2",
    IdentifierSpec.City to "San Francisco",
    IdentifierSpec.State to "CA",
    IdentifierSpec.PostalCode to "94103",
    IdentifierSpec.Country to "US",
)

private val amazonPayTypeOnlyExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.AmazonPay.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.AmazonPay.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val amazonPayFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.AmazonPay.code,
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
            "type" to PaymentMethod.Type.AmazonPay.code,
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

private val amazonPayTypeOnlyWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.AmazonPay.code,
        billingDetails = null,
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.AmazonPay.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val amazonPayFullWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.AmazonPay.code,
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
            "type" to PaymentMethod.Type.AmazonPay.code,
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

internal val amazonPayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay Never PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay AutomaticWithoutTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay Full PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayFullExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay Never PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay AutomaticWithoutTax PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay Full PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay Never SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay AutomaticWithoutTax SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay Full SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay AutomaticWithoutTax PaymentIntentWithSetupFutureUsage never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Amazon Pay AutomaticWithoutTax SetupIntent never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.AmazonPay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = amazonPayRawValues,
        expectedParams = amazonPayTypeOnlyExpectedParams,
    ),
)
