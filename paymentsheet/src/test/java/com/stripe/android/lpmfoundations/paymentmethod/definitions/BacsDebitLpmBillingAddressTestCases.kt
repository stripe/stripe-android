package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val bacsDebitFullRawValues = mapOf(
    IdentifierSpec.Generic("bacs_debit[sort_code]") to "108800",
    IdentifierSpec.Generic("bacs_debit[account_number]") to "00012345",
    IdentifierSpec.BacsDebitConfirmed to "true",
    IdentifierSpec.Name to "Jenny Rosen",
    IdentifierSpec.Email to "jenny.rosen@example.com",
    IdentifierSpec.Line1 to "10 Downing Street",
    IdentifierSpec.Line2 to "Westminster",
    IdentifierSpec.City to "London",
    IdentifierSpec.PostalCode to "SW1A 2AA",
    IdentifierSpec.Country to "GB",
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
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitBankDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Automatic without tax",
        paymentMethodType = PaymentMethod.Type.BacsDebit,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Full",
        paymentMethodType = PaymentMethod.Type.BacsDebit,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
)
