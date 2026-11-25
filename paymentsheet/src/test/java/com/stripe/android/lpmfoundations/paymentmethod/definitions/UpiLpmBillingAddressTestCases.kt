package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val upiAddress = Address(
    line1 = "1 Main Road",
    line2 = "Unit 2",
    city = "Mumbai",
    state = "MH",
    postalCode = "400001",
    country = "IN",
)

internal val upiTestCases = listOf(
    LpmBillingDetailsCollectionMode.Never,
    LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
    LpmBillingDetailsCollectionMode.Full,
).map { mode ->
    val billingDetails = if (mode == LpmBillingDetailsCollectionMode.Full) {
        PaymentMethod.BillingDetails(address = upiAddress)
    } else {
        null
    }
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "UPI ${mode.name}",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Upi,
            billingDetailsCollectionMode = mode,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = mapOf(
            FormFieldId.Line1 to "1 Main Road",
            FormFieldId.Line2 to "Unit 2",
            FormFieldId.City to "Mumbai",
            FormFieldId.State to "MH",
            FormFieldId.PostalCode to "400001",
            FormFieldId.Country to "IN",
        ),
        expectedParams = LpmBillingAddressFormParams(
            createParams = PaymentMethodCreateParams.createWithOverride(
                code = "upi",
                billingDetails = billingDetails,
                requiresMandate = false,
                overrideParamMap = if (billingDetails == null) {
                    mapOf("type" to "upi")
                } else {
                    mapOf(
                        "type" to "upi",
                        "billing_details" to mapOf(
                            "address" to mapOf(
                                "line1" to "1 Main Road",
                                "line2" to "Unit 2",
                                "city" to "Mumbai",
                                "state" to "MH",
                                "postal_code" to "400001",
                                "country" to "IN",
                            ),
                        ),
                    )
                },
                productUsage = emptySet(),
                allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
                clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
            ),
            optionsParams = null,
            extraParams = null,
        ),
    )
}
