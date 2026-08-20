package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
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

private val idealAutomaticWithoutTaxConfig = LpmBillingAddressTestConfiguration(
    paymentMethodType = PaymentMethod.Type.Ideal,
    billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
    intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
    termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = false,
            params = emptyMap(),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "iDEAL AutomaticWithoutTax PaymentIntent automatic terms",
        config = idealAutomaticWithoutTaxConfig,
        rawValues = idealRawValues,
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = false,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = false,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.address.line1" to "Herengracht 1",
                "billing_details.address.line2" to "Appartement 2",
                "billing_details.address.city" to "Amsterdam",
                "billing_details.address.country" to "NL",
                "billing_details.address.postal_code" to "1015BA",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = true,
            params = emptyMap(),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = null,
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = true,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.email" to "sanne.devries@example.com",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = true,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.email" to "sanne.devries@example.com",
                "billing_details.address.line1" to "Herengracht 1",
                "billing_details.address.line2" to "Appartement 2",
                "billing_details.address.city" to "Amsterdam",
                "billing_details.address.country" to "NL",
                "billing_details.address.postal_code" to "1015BA",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = true,
            params = emptyMap(),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = null,
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = true,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.email" to "sanne.devries@example.com",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = true,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.email" to "sanne.devries@example.com",
                "billing_details.address.line1" to "Herengracht 1",
                "billing_details.address.line2" to "Appartement 2",
                "billing_details.address.city" to "Amsterdam",
                "billing_details.address.country" to "NL",
                "billing_details.address.postal_code" to "1015BA",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = false,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.email" to "sanne.devries@example.com",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
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
        expectedParams = expectedFormParams(
            type = PaymentMethod.Type.Ideal,
            requiresMandate = false,
            params = mapOf(
                "billing_details.name" to "Sanne de Vries",
                "billing_details.email" to "sanne.devries@example.com",
            ),
            optionsParams = null,
            extraParams = null,
            allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
            billingDetailsAddressWhenNoAddressParams = Address(),
        ),
    ),
)

internal fun expectedFormParams(
    type: PaymentMethod.Type,
    requiresMandate: Boolean,
    params: Map<String, Any>,
    optionsParams: PaymentMethodOptionsParams?,
    extraParams: PaymentMethodExtraParams?,
    allowRedisplay: PaymentMethod.AllowRedisplay,
    billingDetailsAddressWhenNoAddressParams: Address?,
): LpmBillingAddressFormParams {
    val billingDetails = params.toBillingDetails(billingDetailsAddressWhenNoAddressParams)
    val overrideParamMap = mapOf("type" to type.code) + params.unflatten()

    return LpmBillingAddressFormParams(
        createParams = PaymentMethodCreateParams.createWithOverride(
            code = type.code,
            billingDetails = billingDetails,
            requiresMandate = requiresMandate,
            overrideParamMap = overrideParamMap,
            productUsage = emptySet(),
            allowRedisplay = allowRedisplay,
            clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
        ),
        optionsParams = optionsParams,
        extraParams = extraParams,
    )
}

private fun Map<String, Any>.toBillingDetails(
    billingDetailsAddressWhenNoAddressParams: Address?,
): PaymentMethod.BillingDetails? {
    val billingDetailsPrefix = "billing_details."
    if (keys.none { it.startsWith(billingDetailsPrefix) }) {
        return null
    }

    val addressPrefix = "${billingDetailsPrefix}address."
    val address = if (keys.any { it.startsWith(addressPrefix) }) {
        Address(
            city = get("${addressPrefix}city") as? String,
            country = get("${addressPrefix}country") as? String,
            line1 = get("${addressPrefix}line1") as? String,
            line2 = get("${addressPrefix}line2") as? String,
            postalCode = get("${addressPrefix}postal_code") as? String,
            state = get("${addressPrefix}state") as? String,
        )
    } else {
        billingDetailsAddressWhenNoAddressParams
    }

    return PaymentMethod.BillingDetails(
        address = address,
        email = get("${billingDetailsPrefix}email") as? String,
        name = get("${billingDetailsPrefix}name") as? String,
        phone = get("${billingDetailsPrefix}phone") as? String,
    )
}

private fun Map<String, Any>.unflatten(): Map<String, Any> {
    return entries.fold(linkedMapOf()) { result, (key, value) ->
        val path = key.split(".")
        var nestedMap: MutableMap<String, Any> = result
        path.dropLast(1).forEach { segment ->
            val child = nestedMap[segment]
            if (child == null) {
                val childMap = linkedMapOf<String, Any>()
                nestedMap[segment] = childMap
                nestedMap = childMap
            } else {
                @Suppress("UNCHECKED_CAST")
                nestedMap = child as MutableMap<String, Any>
            }
        }
        nestedMap[path.last()] = value
        result
    }
}
