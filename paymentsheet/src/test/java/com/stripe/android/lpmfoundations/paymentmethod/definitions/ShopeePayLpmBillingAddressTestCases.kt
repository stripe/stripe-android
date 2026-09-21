package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val shopeePayFullRawValues = mapOf(
    FormFieldId.Line1 to "123 Main Street",
    FormFieldId.Line2 to "Unit 2",
    FormFieldId.City to "San Francisco",
    FormFieldId.State to "CA",
    FormFieldId.PostalCode to "94103",
    FormFieldId.Country to "US",
)

private fun shopeePayNoBillingDetailsExpectedPaymentMethodParams(
    requiresMandate: Boolean,
) = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.ShopeePay.code,
    billingDetails = null,
    requiresMandate = requiresMandate,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.ShopeePay.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private fun shopeePayWithBillingAddressExpectedPaymentMethodParams(
    requiresMandate: Boolean,
) = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.ShopeePay.code,
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
        "type" to PaymentMethod.Type.ShopeePay.code,
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

internal val shopeePayTestCases = listOf(
    LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
).flatMap { intentScenario ->
    val requiresMandate = false
    listOf(
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "ShopeePay $intentScenario Never",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.ShopeePay,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = shopeePayFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = shopeePayNoBillingDetailsExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "ShopeePay $intentScenario Automatic without tax",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.ShopeePay,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = shopeePayFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = shopeePayNoBillingDetailsExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "ShopeePay $intentScenario Full",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.ShopeePay,
                billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = shopeePayFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = shopeePayWithBillingAddressExpectedPaymentMethodParams(requiresMandate),
                optionsParams = null,
                extraParams = null,
            ),
        ),
    )
}
