package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val mbWayRawValues = mapOf(
    FormFieldId.Phone to "+351911111112",
    FormFieldId.Line1 to "Avenida da Liberdade 1",
    FormFieldId.Line2 to "",
    FormFieldId.City to "Lisboa",
    FormFieldId.PostalCode to "1250-096",
    FormFieldId.Country to "PT",
)

private fun expectedMbWayParams(
    billingDetails: PaymentMethod.BillingDetails?,
    billingDetailsParamMap: Map<String, Any>? = null,
): PaymentMethodCreateParams {
    return PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.MbWay.code,
        billingDetails = billingDetails,
        requiresMandate = false,
        overrideParamMap = buildMap {
            put("type", PaymentMethod.Type.MbWay.code)
            billingDetailsParamMap?.let { put("billing_details", it) }
        },
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    )
}

internal val mbWayTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MB WAY Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.MbWay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = mbWayRawValues,
        expectedParams = LpmBillingAddressFormParams(expectedMbWayParams(null), null, null),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MB WAY Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.MbWay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = mbWayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            expectedMbWayParams(
                PaymentMethod.BillingDetails(
                    address = Address(),
                    phone = "+351911111112",
                ),
                billingDetailsParamMap = mapOf("phone" to "+351911111112"),
            ),
            null,
            null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "MB WAY Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.MbWay,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = mbWayRawValues,
        expectedParams = LpmBillingAddressFormParams(
            expectedMbWayParams(
                PaymentMethod.BillingDetails(
                    address = Address(
                        line1 = "Avenida da Liberdade 1",
                        line2 = "",
                        city = "Lisboa",
                        postalCode = "1250-096",
                        country = "PT",
                    ),
                    phone = "+351911111112",
                ),
                billingDetailsParamMap = mapOf(
                    "phone" to "+351911111112",
                    "address" to mapOf(
                        "country" to "PT",
                        "line1" to "Avenida da Liberdade 1",
                        "line2" to "",
                        "postal_code" to "1250-096",
                        "city" to "Lisboa",
                    ),
                ),
            ),
            null,
            null,
        ),
    ),
)
