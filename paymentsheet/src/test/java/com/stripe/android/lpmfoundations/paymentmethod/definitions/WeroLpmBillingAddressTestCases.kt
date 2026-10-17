package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val weroCountryOnlyRawValues = mapOf(
    FormFieldId.Country to "DE",
)

private val weroWithBillingAddressRawValues = weroCountryOnlyRawValues + mapOf(
    FormFieldId.Line1 to "Unter den Linden 1",
    FormFieldId.Line2 to "Wohnung 2",
    FormFieldId.City to "Berlin",
    FormFieldId.PostalCode to "10117",
)

private val weroCountryOnlyExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Wero.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(country = "DE"),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Wero.code,
        "billing_details" to mapOf(
            "address" to mapOf("country" to "DE"),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val weroWithBillingAddressExpectedPaymentParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Wero.code,
    billingDetails = PaymentMethod.BillingDetails(
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
        "type" to PaymentMethod.Type.Wero.code,
        "billing_details" to mapOf(
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
)

internal val weroTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Wero Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Wero,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = weroCountryOnlyRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = weroCountryOnlyExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Wero Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Wero,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = weroCountryOnlyRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = weroCountryOnlyExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Wero Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Wero,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = weroWithBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = weroWithBillingAddressExpectedPaymentParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
