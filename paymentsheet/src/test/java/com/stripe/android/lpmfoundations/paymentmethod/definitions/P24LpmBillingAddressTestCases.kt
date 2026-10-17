package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val p24FullRawValues = mapOf(
    FormFieldId.Generic("p24[bank]") to "santander_przelew24",
    FormFieldId.Name to "Anna Kowalska",
    FormFieldId.Email to "anna.kowalska@example.com",
    FormFieldId.Line1 to "Marszalkowska 1",
    FormFieldId.Line2 to "Apartment 2",
    FormFieldId.City to "Warsaw",
    FormFieldId.PostalCode to "00-001",
    FormFieldId.Country to "PL",
)

private val p24NoBillingDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.P24.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.P24.code,
        "p24" to mapOf("bank" to "santander_przelew24"),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val p24WithContactDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.P24.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Anna Kowalska",
        email = "anna.kowalska@example.com",
        address = Address(),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.P24.code,
        "p24" to mapOf("bank" to "santander_przelew24"),
        "billing_details" to mapOf(
            "name" to "Anna Kowalska",
            "email" to "anna.kowalska@example.com",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val p24WithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.P24.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Anna Kowalska",
        email = "anna.kowalska@example.com",
        address = Address(
            line1 = "Marszalkowska 1",
            line2 = "Apartment 2",
            city = "Warsaw",
            country = "PL",
            postalCode = "00-001",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.P24.code,
        "p24" to mapOf("bank" to "santander_przelew24"),
        "billing_details" to mapOf(
            "name" to "Anna Kowalska",
            "email" to "anna.kowalska@example.com",
            "address" to mapOf(
                "line1" to "Marszalkowska 1",
                "line2" to "Apartment 2",
                "city" to "Warsaw",
                "country" to "PL",
                "postal_code" to "00-001",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val p24TestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "P24 Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.P24,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = p24FullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = p24NoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "P24 Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.P24,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = p24FullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = p24WithContactDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "P24 Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.P24,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = p24FullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = p24WithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
