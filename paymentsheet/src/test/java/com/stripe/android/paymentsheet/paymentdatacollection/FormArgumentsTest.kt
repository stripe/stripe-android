package com.stripe.android.paymentsheet.paymentdatacollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.Address
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodExtraParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.IdentifierSpec
import org.junit.Test

class FormArgumentsTest {
    private val billingDetails = PaymentSheet.BillingDetails(
        PaymentSheet.Address(
            line1 = "123 Main Street",
            line2 = "APt 1",
            city = "Dublin",
            state = "Co. Dublin",
            postalCode = "T37 F8HK",
            country = "IE"
        ),
        "email.email.com",
        "Jenny Smith"
    )

    private val parameterMapBillingDetails = "billing_details" to mapOf(
        "address" to mapOf(
            "city" to "Berlin",
            "country" to "DE",
            "line1" to "1234 Main St",
            "line2" to null,
            "state" to "Capital",
            "postal_code" to "10787"
        ),
        "email" to "jenny.rosen@example.com",
        "name" to "Jenny Rosen",
        "phone" to "1-800-555-1234"
    )

    private val paymentMethodCreateParams = PaymentMethodCreateParams.createWithOverride(
        PaymentMethod.Type.Card.code,
        PaymentMethod.Type.Card.requiresMandate,
        mapOf(
            "type" to "card",
            parameterMapBillingDetails,
            "card" to mapOf(
                "number" to "4242424242424242",
                "exp_month" to "1",
                "exp_year" to "2024",
                "cvc" to "111"
            )
        ),
        emptySet()
    )

    @Test
    fun `Verify payment method parameters overrides any billing address values`() {
        val formArguments = FormArguments(
            PaymentMethod.Type.Card.code,
            showCheckbox = true,
            showCheckboxControlledFields = true,
            merchantName = "Merchant, Inc.",
            amount = Amount(50, "USD"),
            billingDetails = billingDetails,
            initialPaymentMethodCreateParams = paymentMethodCreateParams,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(formArguments.getInitialValuesMap()).isEqualTo(
            mapOf(
                IdentifierSpec.Name to "Jenny Rosen",
                IdentifierSpec.Email to "jenny.rosen@example.com",
                IdentifierSpec.Phone to "1-800-555-1234",
                IdentifierSpec.Line1 to "1234 Main St",
                IdentifierSpec.Line2 to null,
                IdentifierSpec.City to "Berlin",
                IdentifierSpec.State to "Capital",
                IdentifierSpec.PostalCode to "10787",
                IdentifierSpec.Country to "DE",
                IdentifierSpec.Generic("type") to "card",
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardExpMonth to "1",
                IdentifierSpec.CardExpYear to "2024",
                IdentifierSpec.CardCvc to "111"
            )
        )
    }

    @Test
    fun `Verify if only default billing address they appear in the initial values`() {
        val formArguments = FormArguments(
            PaymentMethod.Type.Card.code,
            showCheckbox = true,
            showCheckboxControlledFields = true,
            merchantName = "Merchant, Inc.",
            amount = Amount(50, "USD"),
            billingDetails = billingDetails,
            initialPaymentMethodCreateParams = null,
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(formArguments.getInitialValuesMap()).isEqualTo(
            mapOf(
                IdentifierSpec.Name to "Jenny Smith",
                IdentifierSpec.Email to "email.email.com",
                IdentifierSpec.Phone to null,
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "APt 1",
                IdentifierSpec.City to "Dublin",
                IdentifierSpec.State to "Co. Dublin",
                IdentifierSpec.PostalCode to "T37 F8HK",
                IdentifierSpec.Country to "IE"
            )
        )
    }

    @Test
    fun `Verify extra parameters are included if passed in`() {
        val formArguments = FormArguments(
            PaymentMethod.Type.BacsDebit.code,
            showCheckbox = true,
            showCheckboxControlledFields = true,
            merchantName = "Merchant, Inc.",
            amount = Amount(50, "USD"),
            billingDetails = null,
            initialPaymentMethodCreateParams = PaymentMethodCreateParams.create(
                bacsDebit = PaymentMethodCreateParams.BacsDebit(
                    accountNumber = "00012345",
                    sortCode = "10-88-00"
                ),
                billingDetails = PaymentMethod.BillingDetails(
                    name = "Jenny Rosen",
                    email = "jenny.rosen@example.com",
                    address = Address(
                        line1 = "123 Main Street",
                        line2 = "APt 1",
                        city = "Dublin",
                        state = "Co. Dublin",
                        postalCode = "T37 F8HK",
                        country = "IE"
                    )
                )
            ),
            initialPaymentMethodExtraParams = PaymentMethodExtraParams.BacsDebit(
                confirmed = true
            ),
            cbcEligibility = CardBrandChoiceEligibility.Ineligible
        )

        assertThat(formArguments.getInitialValuesMap()).isEqualTo(
            mapOf(
                IdentifierSpec.Name to "Jenny Rosen",
                IdentifierSpec.Email to "jenny.rosen@example.com",
                IdentifierSpec.Phone to null,
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "APt 1",
                IdentifierSpec.City to "Dublin",
                IdentifierSpec.State to "Co. Dublin",
                IdentifierSpec.PostalCode to "T37 F8HK",
                IdentifierSpec.Country to "IE",
                IdentifierSpec.Generic("type") to "bacs_debit",
                IdentifierSpec.Generic("bacs_debit[account_number]") to "00012345",
                IdentifierSpec.Generic("bacs_debit[sort_code]") to "10-88-00",
                IdentifierSpec.BacsDebitConfirmed to "true"
            )
        )
    }
}
