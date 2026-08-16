package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.uicore.elements.IdentifierSpec

private val auBecsDebitFullRawValues = mapOf(
    IdentifierSpec.Generic("au_becs_debit[bsb_number]") to "000000",
    IdentifierSpec.Generic("au_becs_debit[account_number]") to "000123456",
    IdentifierSpec.Name to "Jenny Rosen",
    IdentifierSpec.Email to "jenny.rosen@example.com",
    IdentifierSpec.Line1 to "123 Collins Street",
    IdentifierSpec.Line2 to "Level 4",
    IdentifierSpec.City to "Melbourne",
    IdentifierSpec.State to "VIC",
    IdentifierSpec.PostalCode to "3000",
    IdentifierSpec.Country to "AU",
)

private val auBecsDebitBankDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.AuBecsDebit.code,
    billingDetails = null,
    requiresMandate = true,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.AuBecsDebit.code,
        "au_becs_debit" to mapOf(
            "bsb_number" to "000000",
            "account_number" to "000123456",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val auBecsDebitWithContactDetailsExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.AuBecsDebit.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Jenny Rosen",
        email = "jenny.rosen@example.com",
        address = Address(),
    ),
    requiresMandate = true,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.AuBecsDebit.code,
        "au_becs_debit" to mapOf(
            "bsb_number" to "000000",
            "account_number" to "000123456",
        ),
        "billing_details" to mapOf(
            "name" to "Jenny Rosen",
            "email" to "jenny.rosen@example.com",
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

private val auBecsDebitWithBillingAddressExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.AuBecsDebit.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Jenny Rosen",
        email = "jenny.rosen@example.com",
        address = Address(
            line1 = "123 Collins Street",
            line2 = "Level 4",
            city = "Melbourne",
            state = "VIC",
            country = "AU",
            postalCode = "3000",
        ),
    ),
    requiresMandate = true,
    overrideParamMap = mapOf(
        "type" to PaymentMethod.Type.AuBecsDebit.code,
        "au_becs_debit" to mapOf(
            "bsb_number" to "000000",
            "account_number" to "000123456",
        ),
        "billing_details" to mapOf(
            "name" to "Jenny Rosen",
            "email" to "jenny.rosen@example.com",
            "address" to mapOf(
                "line1" to "123 Collins Street",
                "line2" to "Level 4",
                "city" to "Melbourne",
                "state" to "VIC",
                "country" to "AU",
                "postal_code" to "3000",
            ),
        ),
    ),
    productUsage = emptySet(),
    allowRedisplay = PaymentMethod.AllowRedisplay.UNSPECIFIED,
    clientAttributionMetadata = CLIENT_ATTRIBUTION_METADATA,
)

internal val auBecsDebitTestCases = listOf(
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "AU BECS Debit Never",
        paymentMethodType = PaymentMethod.Type.AuBecsDebit,
        mode = LpmBillingAddressBaselineMode.Never,
        rawValues = auBecsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = auBecsDebitBankDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "AU BECS Debit Automatic without tax",
        paymentMethodType = PaymentMethod.Type.AuBecsDebit,
        mode = LpmBillingAddressBaselineMode.AutomaticWithoutTax,
        rawValues = auBecsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = auBecsDebitWithContactDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "AU BECS Debit Full",
        paymentMethodType = PaymentMethod.Type.AuBecsDebit,
        mode = LpmBillingAddressBaselineMode.Full,
        rawValues = auBecsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = auBecsDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = null,
        ),
    ),
)
