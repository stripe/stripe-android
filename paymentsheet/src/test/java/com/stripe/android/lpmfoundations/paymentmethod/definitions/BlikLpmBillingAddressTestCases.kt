package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val blikFullRawValues = mapOf(
    FormFieldId.BlikCode to "123456",
    FormFieldId.Line1 to "Marszalkowska 1",
    FormFieldId.Line2 to "Apartment 2",
    FormFieldId.City to "Warsaw",
    FormFieldId.PostalCode to "00-001",
    FormFieldId.Country to "PL",
)

private val blikExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Blik.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Blik.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val blikWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Blik.code,
    billingDetails = PaymentMethod.BillingDetails(
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
        "type" to PaymentMethod.Type.Blik.code,
        "billing_details" to mapOf(
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

internal val blikTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Blik Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Blik,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = blikFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = blikExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.Blik("123456"),
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Blik Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Blik,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = blikFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = blikExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.Blik("123456"),
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Blik Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Blik,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = blikFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = blikWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.Blik("123456"),
            extraParams = null,
        ),
    ),
)
