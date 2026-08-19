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

private val idealSetupDefaultBillingDetails = PaymentSheet.BillingDetails(
    name = "Sanne de Vries",
    email = "sanne.devries@example.com",
)

private val idealAutomaticWithoutTaxConfig = LpmBillingAddressTestConfiguration(
    paymentMethodType = PaymentMethod.Type.Ideal,
    billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
    intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
    termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
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

internal val idealTier2TestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL PaymentIntent Automatic without tax",
        config = idealAutomaticWithoutTaxConfig,
        rawValues = idealRawValues,
        expectedParams = idealAutomaticWithoutTaxExpectedParams,
    ),
)

internal val idealTestCases = listOf(
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
        ),
        expectedRequiresMandate = false,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = idealAutomaticWithoutTaxConfig,
        rawValues = idealRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
        ),
        expectedRequiresMandate = false,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.address.line1" to "Herengracht 1",
            "billing_details.address.line2" to "Appartement 2",
            "billing_details.address.city" to "Amsterdam",
            "billing_details.address.country" to "NL",
            "billing_details.address.postal_code" to "1015BA",
        ),
        expectedRequiresMandate = false,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = idealSetupDefaultBillingDetails,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
        ),
        expectedRequiresMandate = true,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
        ),
        expectedRequiresMandate = true,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
            "billing_details.address.line1" to "Herengracht 1",
            "billing_details.address.line2" to "Appartement 2",
            "billing_details.address.city" to "Amsterdam",
            "billing_details.address.country" to "NL",
            "billing_details.address.postal_code" to "1015BA",
        ),
        expectedRequiresMandate = true,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = idealSetupDefaultBillingDetails,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
        ),
        expectedRequiresMandate = true,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
        ),
        expectedRequiresMandate = true,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
            "billing_details.address.line1" to "Herengracht 1",
            "billing_details.address.line2" to "Appartement 2",
            "billing_details.address.city" to "Amsterdam",
            "billing_details.address.country" to "NL",
            "billing_details.address.postal_code" to "1015BA",
        ),
        expectedRequiresMandate = true,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
        ),
        expectedRequiresMandate = false,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
    LpmBillingAddressTier1TestCase(
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Ideal,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = idealSetupRawValues,
        attachedDefaultBillingDetails = null,
        expectedCreateParamsMap = mapOf(
            "type" to PaymentMethod.Type.Ideal.code,
            "allow_redisplay" to PaymentMethod.AllowRedisplay.UNSPECIFIED.value,
            "billing_details.name" to "Sanne de Vries",
            "billing_details.email" to "sanne.devries@example.com",
        ),
        expectedRequiresMandate = false,
        expectedOptionsParams = null,
        expectedExtraParams = null,
    ),
)
