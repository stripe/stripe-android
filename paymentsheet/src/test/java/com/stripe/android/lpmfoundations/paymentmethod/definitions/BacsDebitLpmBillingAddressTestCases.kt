package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet

private val bacsDebitBillingDetails = PaymentSheet.BillingDetails(
    name = "Jenny Rosen",
    email = "jenny.rosen@example.com",
    address = PaymentSheet.Address(
        line1 = "10 Downing Street",
        line2 = "Westminster",
        city = "London",
        postalCode = "SW1A 2AA",
        country = "GB",
    ),
)

private val bacsDebitPaymentMethodCreateParams = PaymentMethodCreateParams.create(
    bacsDebit = PaymentMethodCreateParams.BacsDebit(
        accountNumber = "00012345",
        sortCode = "108800",
    ),
    billingDetails = PaymentMethod.BillingDetails(),
)

private val bacsDebitPaymentMethodExtraParams = PaymentMethodExtraParams.BacsDebit(
    confirmed = true,
)

private val bacsDebitBankDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.BacsDebit.code,
    billingDetails = null,
    requiresMandate = true,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.BacsDebit.code,
        "bacs_debit" to mapOf(
            "sort_code" to "108800",
            "account_number" to "00012345",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val bacsDebitWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.BacsDebit.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Jenny Rosen",
        email = "jenny.rosen@example.com",
        address = Address(
            line1 = "10 Downing Street",
            line2 = "Westminster",
            city = "London",
            country = "GB",
            postalCode = "SW1A2AA",
        ),
    ),
    requiresMandate = true,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.BacsDebit.code,
        "bacs_debit" to mapOf(
            "sort_code" to "108800",
            "account_number" to "00012345",
        ),
        "billing_details" to mapOf(
            "name" to "Jenny Rosen",
            "email" to "jenny.rosen@example.com",
            "address" to mapOf(
                "line1" to "10 Downing Street",
                "line2" to "Westminster",
                "city" to "London",
                "country" to "GB",
                "postal_code" to "SW1A2AA",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val bacsDebitTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Never",
        paymentMethodType = PaymentMethod.Type.BacsDebit,
        mode = LpmBillingAddressBaselineMode.Never,
        defaultBillingDetails = bacsDebitBillingDetails,
        paymentMethodCreateParams = bacsDebitPaymentMethodCreateParams,
        paymentMethodExtraParams = bacsDebitPaymentMethodExtraParams,
        expectedPaymentMethodParams = bacsDebitBankDetailsExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Automatic without tax",
        paymentMethodType = PaymentMethod.Type.BacsDebit,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        defaultBillingDetails = bacsDebitBillingDetails,
        paymentMethodCreateParams = bacsDebitPaymentMethodCreateParams,
        paymentMethodExtraParams = bacsDebitPaymentMethodExtraParams,
        expectedPaymentMethodParams = bacsDebitWithBillingAddressExpectedPaymentMethodParams,
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Full",
        paymentMethodType = PaymentMethod.Type.BacsDebit,
        mode = LpmBillingAddressBaselineMode.Full,
        defaultBillingDetails = bacsDebitBillingDetails,
        paymentMethodCreateParams = bacsDebitPaymentMethodCreateParams,
        paymentMethodExtraParams = bacsDebitPaymentMethodExtraParams,
        expectedPaymentMethodParams = bacsDebitWithBillingAddressExpectedPaymentMethodParams,
    ),
)
