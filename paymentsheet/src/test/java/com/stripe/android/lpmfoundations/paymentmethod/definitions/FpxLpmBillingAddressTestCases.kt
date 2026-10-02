package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val fpxRawValues = mapOf(
    FormFieldId.Generic("fpx[bank]") to "affin_bank",
    FormFieldId.Line1 to "12 Jalan Ampang",
    FormFieldId.Line2 to "Level 3",
    FormFieldId.City to "Kuala Lumpur",
    FormFieldId.State to "Kuala Lumpur",
    FormFieldId.PostalCode to "50450",
    FormFieldId.Country to "MY",
)

private val fpxBankExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Fpx.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Fpx.code,
        "fpx" to mapOf(
            "bank" to "affin_bank",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val fpxWithBillingAddressExpectedParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Fpx.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "12 Jalan Ampang",
            line2 = "Level 3",
            city = "Kuala Lumpur",
            state = "Kuala Lumpur",
            country = "MY",
            postalCode = "50450",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Fpx.code,
        "fpx" to mapOf(
            "bank" to "affin_bank",
        ),
        "billing_details" to mapOf(
            "address" to mapOf(
                "line1" to "12 Jalan Ampang",
                "line2" to "Level 3",
                "city" to "Kuala Lumpur",
                "state" to "Kuala Lumpur",
                "country" to "MY",
                "postal_code" to "50450",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val fpxTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "FPX Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Fpx,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = fpxRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = fpxBankExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "FPX Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Fpx,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = fpxRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = fpxBankExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "FPX Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Fpx,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = fpxRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = fpxWithBillingAddressExpectedParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
