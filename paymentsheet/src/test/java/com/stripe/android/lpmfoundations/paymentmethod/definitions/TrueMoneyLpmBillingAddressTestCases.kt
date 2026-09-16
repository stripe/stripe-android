package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val trueMoneyFullRawValues = mapOf(
    FormFieldId.Line1 to "123 Main Street",
    FormFieldId.Line2 to "Unit 2",
    FormFieldId.City to "San Francisco",
    FormFieldId.State to "CA",
    FormFieldId.PostalCode to "94103",
    FormFieldId.Country to "US",
)

private fun trueMoneyNoBillingDetailsExpectedPaymentMethodParams(
    requiresMandate: Boolean,
) = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.TrueMoney.code,
    billingDetails = null,
    requiresMandate = requiresMandate,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.TrueMoney.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private fun trueMoneyWithBillingAddressExpectedPaymentMethodParams(
    requiresMandate: Boolean,
) = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.TrueMoney.code,
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
        "type" to PaymentMethod.Type.TrueMoney.code,
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

internal val trueMoneyTestCases = LpmBillingAddressTestConfiguration.IntentScenario.entries.flatMap { intentScenario ->
    val requiresMandate = intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent
    listOf(
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "TrueMoney $intentScenario Never",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.TrueMoney,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = trueMoneyFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = trueMoneyNoBillingDetailsExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "TrueMoney $intentScenario Automatic without tax",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.TrueMoney,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = trueMoneyFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = trueMoneyNoBillingDetailsExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "TrueMoney $intentScenario Full",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.TrueMoney,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = trueMoneyFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = trueMoneyWithBillingAddressExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
    )
}
