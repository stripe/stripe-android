package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val sequraFullRawValues = mapOf(
    FormFieldId.Line1 to "Gran Vía 1",
    FormFieldId.Line2 to "Piso 2",
    FormFieldId.City to "Madrid",
    FormFieldId.State to "Madrid",
    FormFieldId.PostalCode to "28013",
    FormFieldId.Country to "ES",
)

private val sequraNoBillingDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Sequra.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Sequra.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val sequraWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Sequra.code,
    billingDetails = PaymentMethod.BillingDetails(
        address = Address(
            line1 = "Gran Vía 1",
            line2 = "Piso 2",
            city = "Madrid",
            state = "Madrid",
            country = "ES",
            postalCode = "28013",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Sequra.code,
        "billing_details" to mapOf(
            "address" to mapOf(
                "line1" to "Gran Vía 1",
                "line2" to "Piso 2",
                "city" to "Madrid",
                "state" to "Madrid",
                "country" to "ES",
                "postal_code" to "28013",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val sequraTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Sequra Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Sequra,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = sequraFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = sequraNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Sequra Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Sequra,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = sequraFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = sequraNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Sequra Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Sequra,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = sequraFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = sequraWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
