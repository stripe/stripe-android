package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val almaRawValues = mapOf(
    IdentifierSpec.Line1 to "12 Rue de la Paix",
    IdentifierSpec.Line2 to "Appartement 4",
    IdentifierSpec.City to "Paris",
    IdentifierSpec.PostalCode to "75002",
    IdentifierSpec.Country to "FR",
)

private val almaNeverExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Alma.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Alma.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val almaAutomaticWithoutTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Alma.code,
        billingDetails = null,
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Alma.code,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    ),
    optionsParams = null,
    extraParams = null,
)

private val almaAutomaticWithTaxExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Alma.code,
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(
                line2 = null,
                country = "FR",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Alma.code,
            "billing_details" to mapOf(
                "address" to mapOf(
                    "country" to "FR",
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

private val almaFullExpectedParams = LpmBillingAddressFormParams(
    createParams = PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Alma.code,
        billingDetails = PaymentMethod.BillingDetails(
            address = Address(
                line1 = "12 Rue de la Paix",
                line2 = "Appartement 4",
                city = "Paris",
                country = "FR",
                postalCode = "75002",
            ),
        ),
        requiresMandate = false,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Alma.code,
            "billing_details" to mapOf(
                "address" to mapOf(
                    "line1" to "12 Rue de la Paix",
                    "line2" to "Appartement 4",
                    "city" to "Paris",
                    "country" to "FR",
                    "postal_code" to "75002",
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

internal val almaTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Alma Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Alma,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = almaRawValues,
        expectedParams = almaNeverExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Alma Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Alma,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = almaRawValues,
        expectedParams = almaAutomaticWithoutTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Alma AutomaticWithTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Alma,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = almaRawValues,
        expectedParams = almaAutomaticWithTaxExpectedParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Alma Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Alma,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = almaRawValues,
        expectedParams = almaFullExpectedParams,
    ),
)
