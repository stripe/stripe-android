package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val idealRawValues = mapOf(
    IdentifierSpec.Name to "Sanne de Vries",
    IdentifierSpec.Line1 to "Herengracht 1",
    IdentifierSpec.Line2 to "Appartement 2",
    IdentifierSpec.City to "Amsterdam",
    IdentifierSpec.PostalCode to "1015 BA",
    IdentifierSpec.Country to "NL",
)

private val idealSetupRawValues = idealRawValues + (
    IdentifierSpec.Email to "sanne.devries@example.com"
)

private val idealNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val idealAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Sanne de Vries",
            address = Address(),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "billing_details" to mapOf(
                "name" to "Sanne de Vries",
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val idealFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Sanne de Vries",
            address = Address(
                line1 = "Herengracht 1",
                line2 = "Appartement 2",
                city = "Amsterdam",
                country = "NL",
                postalCode = "1015BA",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "billing_details" to mapOf(
                "name" to "Sanne de Vries",
                "address" to mapOf(
                    "line1" to "Herengracht 1",
                    "line2" to "Appartement 2",
                    "city" to "Amsterdam",
                    "country" to "NL",
                    "postal_code" to "1015BA",
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

private val idealNeverWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = null,
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val idealAutomaticWithoutTaxWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Sanne de Vries",
            email = "sanne.devries@example.com",
            address = Address(),
        ),
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "billing_details" to mapOf(
                "name" to "Sanne de Vries",
                "email" to "sanne.devries@example.com",
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val idealFullWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Sanne de Vries",
            email = "sanne.devries@example.com",
            address = Address(
                line1 = "Herengracht 1",
                line2 = "Appartement 2",
                city = "Amsterdam",
                country = "NL",
                postalCode = "1015BA",
            ),
        ),
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "billing_details" to mapOf(
                "name" to "Sanne de Vries",
                "email" to "sanne.devries@example.com",
                "address" to mapOf(
                    "line1" to "Herengracht 1",
                    "line2" to "Appartement 2",
                    "city" to "Amsterdam",
                    "country" to "NL",
                    "postal_code" to "1015BA",
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

private val idealAutomaticWithoutTaxWithoutMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Ideal.code,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Sanne de Vries",
            email = "sanne.devries@example.com",
            address = Address(),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "billing_details" to mapOf(
                "name" to "Sanne de Vries",
                "email" to "sanne.devries@example.com",
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

internal val idealFormParamsTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL Never PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealRawValues,
        expectedParams = idealNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL AutomaticWithoutTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealRawValues,
        expectedParams = idealAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL Full PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealRawValues,
        expectedParams = idealFullExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL Never PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealNeverWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL AutomaticWithoutTax PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealAutomaticWithoutTaxWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL Full PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL Never SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealNeverWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL AutomaticWithoutTax SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealAutomaticWithoutTaxWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL Full SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL AutomaticWithoutTax PaymentIntentWithSetupFutureUsage never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealAutomaticWithoutTaxWithoutMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL AutomaticWithoutTax SetupIntent never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = idealSetupRawValues,
        expectedParams = idealAutomaticWithoutTaxWithoutMandateExpectedParams,
    ),
)
