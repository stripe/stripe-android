package com.stripe.android.lpmfoundations.paymentmethod.definitions

import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFixtures.CLIENT_ATTRIBUTION_METADATA
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.uicore.elements.FormFieldId

private val bacsDebitFullRawValues = mapOf(
    FormFieldId.Generic("bacs_debit[sort_code]") to "108800",
    FormFieldId.Generic("bacs_debit[account_number]") to "00012345",
    FormFieldId.BacsDebitConfirmed to "true",
    FormFieldId.Name to "Jenny Rosen",
    FormFieldId.Email to "jenny.rosen@example.com",
    FormFieldId.Line1 to "10 Downing Street",
    FormFieldId.Line2 to "Westminster",
    FormFieldId.City to "London",
    FormFieldId.PostalCode to "SW1A 2AA",
    FormFieldId.Country to "GB",
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

private val bacsDebitAutomaticWithTaxExpectedPaymentMethodParams = PaymentMethodCreateParams.createWithOverride(
    code = PaymentMethod.Type.BacsDebit.code,
    billingDetails = PaymentMethod.BillingDetails(
        name = "Jenny Rosen",
        email = "jenny.rosen@example.com",
        address = Address(
            line2 = null,
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
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.BacsDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Never,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitBankDetailsExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Automatic without tax",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.BacsDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithoutTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit AutomaticWithTax PaymentIntent automatic terms",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.BacsDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.AutomaticWithTax,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitAutomaticWithTaxExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
    LpmBillingAddressFormValuesToParamsTestCase(
        name = "Bacs Debit Full",
        config = LpmBillingAddressTestConfiguration(
            paymentMethodType = PaymentMethod.Type.BacsDebit,
            billingDetailsCollectionMode = LpmBillingDetailsCollectionMode.Full,
            intentScenario = LpmBillingAddressTestConfiguration.IntentScenario.PaymentIntent,
            termsDisplay = PaymentSheet.TermsDisplay.AUTOMATIC,
        ),
        rawValues = bacsDebitFullRawValues,
        expectedParams = LpmBillingAddressFormParams(
            createParams = bacsDebitWithBillingAddressExpectedPaymentMethodParams,
            optionsParams = null,
            extraParams = PaymentMethodExtraParams.BacsDebit(confirmed = true),
        ),
    ),
)
