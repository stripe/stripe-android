package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val pixTaxIdFieldId = FormFieldId.Generic("billing_details[tax_id]")

private val pixFullRawValues = mapOf(
    FormFieldId.Name to "Jenny Rosen",
    FormFieldId.Email to "jenny@example.com",
    pixTaxIdFieldId to "12345678909",
    FormFieldId.Line1 to "123 Main Street",
    FormFieldId.Line2 to "Unit 2",
    FormFieldId.City to "San Francisco",
    FormFieldId.State to "CA",
    FormFieldId.PostalCode to "94103",
    FormFieldId.Country to "US",
)

private fun pixExpectedPaymentMethodParams(
    requiresMandate: Boolean,
    mode: LpmBillingDetailsCollectionMode,
): PaymentMethodCreateParams {
    val address = Address(
        line1 = "123 Main Street",
        line2 = "Unit 2",
        city = "San Francisco",
        state = "CA",
        country = "US",
        postalCode = "94103",
    )
    val billingDetails = when (mode) {
        LpmBillingDetailsCollectionMode.Never -> null
        LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
        LpmBillingDetailsCollectionMode.AutomaticWithTax,
        -> PaymentMethod.BillingDetails(
            name = "Jenny Rosen",
            email = "jenny@example.com",
            address = Address(),
        )
        LpmBillingDetailsCollectionMode.Full -> PaymentMethod.BillingDetails(
            name = "Jenny Rosen",
            email = "jenny@example.com",
            address = address,
        )
    }
    val billingDetailsParams = buildMap<String, Any> {
        put("tax_id", "12345678909")
        if (mode != LpmBillingDetailsCollectionMode.Never) {
            put("name", "Jenny Rosen")
            put("email", "jenny@example.com")
        }
        if (mode == LpmBillingDetailsCollectionMode.Full) {
            put(
                "address",
                mapOf(
                    "line1" to "123 Main Street",
                    "line2" to "Unit 2",
                    "city" to "San Francisco",
                    "state" to "CA",
                    "country" to "US",
                    "postal_code" to "94103",
                )
            )
        }
    }

    return PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Pix.code,
        billingDetails = billingDetails,
        requiresMandate = requiresMandate,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.Pix.code,
            "billing_details" to billingDetailsParams,
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    )
}

internal val pixTestCases = LpmBillingAddressTestConfiguration.IntentScenario.entries.flatMap { intentScenario ->
    val requiresMandate = intentScenario != LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent

    listOf(
        LpmBillingDetailsCollectionMode.Never,
        LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
        LpmBillingDetailsCollectionMode.Full,
    ).map { billingMode ->
        LpmBillingAddressFormValuesToParamsTestCase(
            name = "Pix $intentScenario $billingMode",
            config = LpmBillingAddressTestConfiguration(
                paymentMethodType = PaymentMethod.Type.Pix,
                billingDetailsCollectionMode = billingMode,
                intentScenario = intentScenario,
                termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
            ),
            rawValues = pixFullRawValues,
            expectedParams = LpmBillingAddressFormParams(
                createParams = pixExpectedPaymentMethodParams(requiresMandate, billingMode),
                optionsParams = null,
                extraParams = null,
            ),
        )
    }
}
