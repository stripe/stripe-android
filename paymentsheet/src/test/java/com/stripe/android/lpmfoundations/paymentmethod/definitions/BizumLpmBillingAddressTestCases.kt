package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val bizumRawValues = mapOf(
    FormFieldId.Phone to "+34600000001",
    FormFieldId.Line1 to "Calle de Alcalá 1",
    FormFieldId.Line2 to "",
    FormFieldId.City to "Madrid",
    FormFieldId.State to "MD",
    FormFieldId.PostalCode to "28014",
    FormFieldId.Country to "ES",
)

private fun expectedBizumParams(
    billingDetails: PaymentMethod.BillingDetails?,
    billingDetailsParamMap: Map<String, Any>? = null,
): PaymentMethodCreateParams {
    return PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.Bizum.code,
        billingDetails = billingDetails,
        requiresMandate = false,
        overrideParamMap = buildMap {
            put("type", PaymentMethod.Type.Bizum.code)
            billingDetailsParamMap?.let { put("billing_details", it) }
        },
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    )
}

internal val bizumTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bizum Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Bizum,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bizumRawValues,
        expectedParams = LpmBillingAddressFormParams(expectedBizumParams(null), null, null),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bizum Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Bizum,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bizumRawValues,
        expectedParams = LpmBillingAddressFormParams(
            expectedBizumParams(
                PaymentMethod.BillingDetails(
                    address = Address(),
                    phone = "+34600000001",
                ),
                billingDetailsParamMap = mapOf("phone" to "+34600000001"),
            ),
            null,
            null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bizum Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Bizum,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bizumRawValues,
        expectedParams = LpmBillingAddressFormParams(
            expectedBizumParams(
                PaymentMethod.BillingDetails(
                    address = Address(
                        line1 = "Calle de Alcalá 1",
                        line2 = "",
                        city = "Madrid",
                        state = "MD",
                        postalCode = "28014",
                        country = "ES",
                    ),
                    phone = "+34600000001",
                ),
                billingDetailsParamMap = mapOf(
                    "phone" to "+34600000001",
                    "address" to mapOf(
                        "country" to "ES",
                        "line1" to "Calle de Alcalá 1",
                        "line2" to "",
                        "postal_code" to "28014",
                        "city" to "Madrid",
                        "state" to "MD",
                    ),
                ),
            ),
            null,
            null,
        ),
    ),
)
