package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val moMoFullRawValues = mapOf(
    FormFieldId.Line1 to "123 Main Street",
    FormFieldId.Line2 to "Unit 2",
    FormFieldId.City to "San Francisco",
    FormFieldId.State to "CA",
    FormFieldId.PostalCode to "94103",
    FormFieldId.Country to "US",
)

private fun moMoNoBillingDetailsExpectedPaymentMethodParams() = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.MoMo.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.MoMo.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private fun moMoWithBillingAddressExpectedPaymentMethodParams() = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.MoMo.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "123 Main Street",
            line2 = "Unit 2",
            city = "San Francisco",
            state = "CA",
            country = "US",
            postalCode = "94103",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.MoMo.code,
        "billing_details" to mapOf(
            "address" to mapOf(
                "line1" to "123 Main Street",
                "line2" to "Unit 2",
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
)

internal val moMoTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MoMo PaymentIntent Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.MoMo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = moMoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = moMoNoBillingDetailsExpectedPaymentMethodParams(),
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MoMo PaymentIntent Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.MoMo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = moMoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = moMoNoBillingDetailsExpectedPaymentMethodParams(),
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MoMo PaymentIntent Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.MoMo,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = moMoFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = moMoWithBillingAddressExpectedPaymentMethodParams(),
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
