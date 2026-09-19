package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val ngCardFullRawValues = mapOf(
    FormFieldId.Line1 to "123 Main Street",
    FormFieldId.Line2 to "Unit 2",
    FormFieldId.City to "San Francisco",
    FormFieldId.State to "CA",
    FormFieldId.PostalCode to "94103",
    FormFieldId.Country to "US",
)

private fun ngCardNoBillingDetailsExpectedPaymentMethodParams(
    requiresMandate: Boolean,
) = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.NgCard.code,
    billingDetails = null,
    requiresMandate = requiresMandate,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.NgCard.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private fun ngCardWithBillingAddressExpectedPaymentMethodParams(
    requiresMandate: Boolean,
) = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.NgCard.code,
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
    requiresMandate = requiresMandate,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.NgCard.code,
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

internal val ngCardTestCases = LpmBillingAddressTestConfiguration.IntentScenario.entries.flatMap { intentScenario ->
    val requiresMandate = intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
    listOf(
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "Naira card $intentScenario Never",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.NgCard,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = ngCardFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = ngCardNoBillingDetailsExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "Naira card $intentScenario Automatic without tax",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.NgCard,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = ngCardFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = ngCardNoBillingDetailsExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "Naira card $intentScenario Full",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.NgCard,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = ngCardFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = ngCardWithBillingAddressExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
    )
}
