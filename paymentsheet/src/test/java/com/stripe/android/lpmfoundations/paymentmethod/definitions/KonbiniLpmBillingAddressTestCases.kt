package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val konbiniFullRawValues = mapOf(
    FormFieldId.Name to "Haruto Tanaka",
    FormFieldId.Email to "haruto.tanaka@example.com",
    FormFieldId.KonbiniConfirmationNumber to "09012345678",
    FormFieldId.Line1 to "1-1 Marunouchi",
    FormFieldId.Line2 to "Chiyoda Building 2F",
    FormFieldId.State to "Tokyo",
    FormFieldId.PostalCode to "100-0005",
    FormFieldId.Country to "JP",
)

private val konbiniNoBillingDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Konbini.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Konbini.code,
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val konbiniWithContactDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Konbini.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Haruto Tanaka",
        email = "haruto.tanaka@example.com",
        address = Address(),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Konbini.code,
        "billing_details" to mapOf(
            "name" to "Haruto Tanaka",
            "email" to "haruto.tanaka@example.com",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val konbiniWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Konbini.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Haruto Tanaka",
        email = "haruto.tanaka@example.com",
        address = Address(
            line1 = "1-1 Marunouchi",
            line2 = "Chiyoda Building 2F",
            state = "Tokyo",
            country = "JP",
            postalCode = "100-0005",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Konbini.code,
        "billing_details" to mapOf(
            "name" to "Haruto Tanaka",
            "email" to "haruto.tanaka@example.com",
            "address" to mapOf(
                "line1" to "1-1 Marunouchi",
                "line2" to "Chiyoda Building 2F",
                "state" to "Tokyo",
                "country" to "JP",
                "postal_code" to "100-0005",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val konbiniExpectedOptionsParams = PaymentMethodOptionsParams.Konbini(
    confirmationNumber = "09012345678",
)

internal val konbiniTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Konbini Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Konbini,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = konbiniFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = konbiniNoBillingDetailsExpectedPaymentMethodParams,
            optionsParams = konbiniExpectedOptionsParams,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Konbini Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Konbini,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = konbiniFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = konbiniWithContactDetailsExpectedPaymentMethodParams,
            optionsParams = konbiniExpectedOptionsParams,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Konbini Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Konbini,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = konbiniFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = konbiniWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = konbiniExpectedOptionsParams,
            extraParams = null,
        ),
    ),
)
