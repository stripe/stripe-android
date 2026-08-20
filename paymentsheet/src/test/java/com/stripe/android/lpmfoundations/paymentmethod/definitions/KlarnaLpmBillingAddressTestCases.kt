package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val klarnaRawValues = mapOf(
    IdentifierSpec.Country to "DE",
    IdentifierSpec.Email to "jane@example.com",
    IdentifierSpec.Line1 to "Unter den Linden 1",
    IdentifierSpec.Line2 to "Wohnung 2",
    IdentifierSpec.City to "Berlin",
    IdentifierSpec.PostalCode to "10117",
)

private val klarnaNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(country = "DE"),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "address" to mapOf("country" to "DE"),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val klarnaAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "jane@example.com",
            address = Address(country = "DE"),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "email" to "jane@example.com",
                "address" to mapOf("country" to "DE"),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val klarnaFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "jane@example.com",
            address = Address(
                line1 = "Unter den Linden 1",
                line2 = "Wohnung 2",
                city = "Berlin",
                country = "DE",
                postalCode = "10117",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "email" to "jane@example.com",
                "address" to mapOf(
                    "line1" to "Unter den Linden 1",
                    "line2" to "Wohnung 2",
                    "city" to "Berlin",
                    "country" to "DE",
                    "postal_code" to "10117",
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

private val klarnaNeverWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(country = "DE"),
        ),
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "address" to mapOf("country" to "DE"),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val klarnaAutomaticWithoutTaxWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "jane@example.com",
            address = Address(country = "DE"),
        ),
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "email" to "jane@example.com",
                "address" to mapOf("country" to "DE"),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val klarnaFullWithMandateExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Klarna.code,
        billingDetails = PaymentMethod.BillingDetails(
            email = "jane@example.com",
            address = Address(
                line1 = "Unter den Linden 1",
                line2 = "Wohnung 2",
                city = "Berlin",
                country = "DE",
                postalCode = "10117",
            ),
        ),
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Klarna.code,
            "billing_details" to mapOf(
                "email" to "jane@example.com",
                "address" to mapOf(
                    "line1" to "Unter den Linden 1",
                    "line2" to "Wohnung 2",
                    "city" to "Berlin",
                    "country" to "DE",
                    "postal_code" to "10117",
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

internal val klarnaTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Never PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna AutomaticWithoutTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Full PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaFullExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Never PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaNeverWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna AutomaticWithoutTax PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaAutomaticWithoutTaxWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Full PaymentIntentWithSetupFutureUsage automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Never SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaNeverWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna AutomaticWithoutTax SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaAutomaticWithoutTaxWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna Full SetupIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaFullWithMandateExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna AutomaticWithoutTax PaymentIntentWithSetupFutureUsage never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntentWithSetupFutureUsage,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Klarna AutomaticWithoutTax SetupIntent never terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Klarna,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.SetupIntent,
            termsDisplay = PaymentSheet.TermsDisplay.NEVER,
        ),
        rawValues = klarnaRawValues,
        expectedParams = klarnaAutomaticWithoutTaxExpectedParams,
    ),
)
