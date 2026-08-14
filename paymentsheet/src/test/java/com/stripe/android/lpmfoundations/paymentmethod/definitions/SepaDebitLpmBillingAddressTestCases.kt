package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet

private val sepaDebitBillingDetails = PaymentSheet.BillingDetails(
    name = "Jane Doe",
    email = "jane@example.com",
    address = PaymentSheet.Address(
        line1 = "Unter den Linden 1",
        line2 = "Wohnung 2",
        city = "Berlin",
        country = "DE",
        postalCode = "10117",
    ),
)

private val sepaDebitPaymentMethodCreateParams = PaymentMethodCreateParams.create(
    sepaDebit = PaymentMethodCreateParams.SepaDebit(
        iban = "DE89370400440532013000",
    ),
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
        paymentMethodType = PaymentMethod.Type.SepaDebit,
        mode = LpmBillingAddressBaselineMode.Never,
        defaultBillingDetails = sepaDebitBillingDetails,
        paymentMethodCreateParams = sepaDebitPaymentMethodCreateParams,
        paymentMethodExtraParams = null,
        expectedPaymentMethodParams = sepaDebitNoBillingAddressExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "SEPA Debit Automatic without tax",
        paymentMethodType = PaymentMethod.Type.SepaDebit,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        defaultBillingDetails = sepaDebitBillingDetails,
        paymentMethodCreateParams = sepaDebitPaymentMethodCreateParams,
        paymentMethodExtraParams = null,
        expectedPaymentMethodParams = sepaDebitWithBillingAddressExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "SEPA Debit Full",
        paymentMethodType = PaymentMethod.Type.SepaDebit,
        mode = LpmBillingAddressBaselineMode.Full,
        defaultBillingDetails = sepaDebitBillingDetails,
        paymentMethodCreateParams = sepaDebitPaymentMethodCreateParams,
        paymentMethodExtraParams = null,
        expectedPaymentMethodParams = sepaDebitWithBillingAddressExpectedPaymentMethodParams,
    ),
)
