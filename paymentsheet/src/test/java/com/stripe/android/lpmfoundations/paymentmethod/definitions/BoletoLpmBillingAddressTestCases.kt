package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val boletoNoBillingAddressRawValues = mapOf(
    IdentifierSpec.Generic("boleto[tax_id]") to "123.456.789-09",
)

private val boletoWithBillingAddressRawValues = boletoNoBillingAddressRawValues + mapOf(
    IdentifierSpec.Name to "Jane Doe",
    IdentifierSpec.Email to "jane@example.com",
    IdentifierSpec.Line1 to "Avenida Paulista 123",
    IdentifierSpec.Line2 to "Apto 45",
    IdentifierSpec.City to "Sao Paulo",
    IdentifierSpec.State to "SP",
    IdentifierSpec.Country to "BR",
    IdentifierSpec.PostalCode to "01311000",
)

private val boletoNoBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Boleto.code,
    billingDetails = null,
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Boleto.code,
        "boleto" to mapOf("tax_id" to "123.456.789-09"),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val boletoWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.Boleto.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Jane Doe",
        email = "jane@example.com",
        address = Address(
            line1 = "Avenida Paulista 123",
            line2 = "Apto 45",
            city = "Sao Paulo",
            state = "SP",
            country = "BR",
            postalCode = "01311000",
        ),
    ),
    requiresMandate = false,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.Boleto.code,
        "boleto" to mapOf("tax_id" to "123.456.789-09"),
        "billing_details" to mapOf(
            "name" to "Jane Doe",
            "email" to "jane@example.com",
            "address" to mapOf(
                "line1" to "Avenida Paulista 123",
                "line2" to "Apto 45",
                "city" to "Sao Paulo",
                "state" to "SP",
                "country" to "BR",
                "postal_code" to "01311000",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val boletoTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Boleto Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Boleto,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = boletoNoBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = boletoNoBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Boleto Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Boleto,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = boletoWithBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = boletoWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Boleto Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.Boleto,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = boletoWithBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = boletoWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
