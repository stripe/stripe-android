package com.stripe.android.paymentsheet.paymentdatacollection

import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.forms.PaymentMethodRequirements
import com.stripe.android.ui.core.Amount
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.elements.CardBillingSpec
import com.stripe.android.ui.core.elements.CardDetailsSectionSpec
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.forms.resources.LpmRepository
import org.junit.Test

class FormFragmentArgumentsTest {
    private val card = LpmRepository.SupportedPaymentMethod(
        PaymentMethod.Type.Card,
        R.string.stripe_paymentsheet_payment_method_card,
        R.drawable.stripe_ic_paymentsheet_pm_card,
        PaymentMethodRequirements(
            piRequirements = emptySet(),
            siRequirements = emptySet(),
            confirmPMFromCustomer = true
        ),
        LayoutSpec(listOf(CardDetailsSectionSpec(), CardBillingSpec()))
    )

    private val billingDetails = PaymentSheet.BillingDetails(
        PaymentSheet.Address(
            line1 = "123 Main Street",
            line2 = "APt 1",
            city = "Dublin",
            state = "Co. Dublin",
            postalCode = "T37 F8HK",
            country = "IE",

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
        PaymentMethod.Type.Card,
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
        val formFragmentArguments = FormFragmentArguments(
            card,
            showCheckbox = true,
            showCheckboxControlledFields = true,
            merchantName = "Merchant, Inc.",
            amount = Amount(50, "USD"),
            billingDetails = billingDetails,
            injectorKey = "injectorTestKeyFormFragmentArgumentTest",
            initialPaymentMethodCreateParams = paymentMethodCreateParams
        )

        assertThat(formFragmentArguments.getInitialValuesMap()).isEqualTo(
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
                IdentifierSpec.Generic("card[exp_month]") to "1",
                IdentifierSpec.Generic("card[exp_year]") to "2024",
                IdentifierSpec.CardCvc to "111"
            )
        )
    }

    @Test
    fun `Verify if only default billing address they appear in the initial values`() {
        val formFragmentArguments = FormFragmentArguments(
            card,
            showCheckbox = true,
            showCheckboxControlledFields = true,
            merchantName = "Merchant, Inc.",
            amount = Amount(50, "USD"),
            billingDetails = billingDetails,
            injectorKey = "injectorTestKeyFormFragmentArgumentTest",
            initialPaymentMethodCreateParams = null
        )

        assertThat(formFragmentArguments.getInitialValuesMap()).isEqualTo(
            mapOf(
                IdentifierSpec.Name to "Jenny Smith",
                IdentifierSpec.Email to "email.email.com",
                IdentifierSpec.Phone to null,
                IdentifierSpec.Line1 to "123 Main Street",
                IdentifierSpec.Line2 to "APt 1",
                IdentifierSpec.City to "Dublin",
                IdentifierSpec.State to "Co. Dublin",
                IdentifierSpec.PostalCode to "T37 F8HK",
                IdentifierSpec.Country to "IE",
            )
        )
    }
}
