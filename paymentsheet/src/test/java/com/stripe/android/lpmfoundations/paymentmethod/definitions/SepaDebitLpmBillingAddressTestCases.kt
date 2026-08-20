package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.IdentifierSpec

private val sepaDebitNoBillingAddressRawValues = mapOf(
    IdentifierSpec.Generic("sepa_debit[iban]") to "DE89370400440532013000",
)

private val sepaDebitWithBillingAddressRawValues = sepaDebitNoBillingAddressRawValues + mapOf(
    IdentifierSpec.Name to "Jane Doe",
    IdentifierSpec.Email to "jane@example.com",
    IdentifierSpec.Line1 to "Unter den Linden 1",
    IdentifierSpec.Line2 to "Wohnung 2",
    IdentifierSpec.City to "Berlin",
    IdentifierSpec.Country to "DE",
    IdentifierSpec.PostalCode to "10117",
)

private val sepaDebitNoBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.SepaDebit.code,
    billingDetails = null,
    requiresMandate = true,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.SepaDebit.code,
        "sepa_debit" to mapOf("iban" to "DE89370400440532013000"),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val sepaDebitWithBillingAddressExpectedPaymentMethodParams =
    PaymentMethodCreateParams.createWithOverride(
        code = PaymentMethod.Type.SepaDebit.code,
        billingDetails = PaymentMethod.BillingDetails(
            name = "Jane Doe",
            email = "jane@example.com",
            address = Address(
                line1 = "Unter den Linden 1",
                line2 = "Wohnung 2",
                city = "Berlin",
                country = "DE",
                postalCode = "10117",
            ),
        ),
        requiresMandate = true,
        overrideParamMap = mapOf(
            "type" to PaymentMethod.Type.SepaDebit.code,
            "sepa_debit" to mapOf("iban" to "DE89370400440532013000"),
            "billing_details" to mapOf(
                "name" to "Jane Doe",
                "email" to "jane@example.com",
                "address" to mapOf(
                    "line1" to "Unter den Linden 1",
                    "line2" to "Wohnung 2",
                    "city" to "Berlin",
                    "country" to "DE",
                    "postal_code" to "10117",
                ),
            ),
        ),
        productUsage = emptySet(),
        allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
        clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
    )

internal val sepaDebitTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "SEPA Debit Never",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.SepaDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = sepaDebitNoBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = sepaDebitNoBillingAddressExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.SepaDebit(null),
            extraParams = PaymentMethodExtraParams.SepaDebit(null),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "SEPA Debit Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.SepaDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = sepaDebitWithBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = sepaDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.SepaDebit(null),
            extraParams = PaymentMethodExtraParams.SepaDebit(null),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "SEPA Debit Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.SepaDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = sepaDebitWithBillingAddressRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = sepaDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = PaymentMethodOptionsParams.SepaDebit(null),
            extraParams = PaymentMethodExtraParams.SepaDebit(null),
        ),
    ),
)
