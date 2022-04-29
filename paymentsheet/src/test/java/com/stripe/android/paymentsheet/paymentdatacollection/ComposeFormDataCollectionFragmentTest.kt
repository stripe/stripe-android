package com.stripe.android.paymentsheet.paymentdatacollection

import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.BaseAddPaymentMethodFragment
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test

class ComposeFormDataCollectionFragmentTest {

    @Test
    fun `card payment method selection has the fields from formFieldValues`() {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.SaveForFutureUse to FormFieldEntry("true", true),
                IdentifierSpec.CardNumber to FormFieldEntry("4242424242421234", true),
                IdentifierSpec.CardBrand to FormFieldEntry(CardBrand.Visa.code, true)
            ),
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        val selection =
            ComposeFormDataCollectionFragment().transformToPaymentSelection(
                formFieldValues,
                SupportedPaymentMethod.Card
            )
        assertThat(selection?.customerRequestedSave).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        assertThat((selection as? PaymentSelection.New.Card)?.last4).isEqualTo(
            "1234"
        )
        assertThat((selection as? PaymentSelection.New.Card)?.brand).isEqualTo(
            CardBrand.Visa
        )
    }

    @Test
    fun `payment method selection has the fields from formFieldValues`() {
        val formFieldValues = FormFieldValues(
            fieldValuePairs = mapOf(
                IdentifierSpec.SaveForFutureUse to FormFieldEntry("true", true)
            ),
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        val selection =
            ComposeFormDataCollectionFragment().transformToPaymentSelection(
                formFieldValues,
                SupportedPaymentMethod.Sofort
            )
        assertThat(selection?.customerRequestedSave).isEqualTo(
            PaymentSelection.CustomerRequestedSave.RequestReuse
        )
        assertThat((selection as? PaymentSelection.New.GenericPaymentMethod)?.labelResource).isEqualTo(
            R.string.stripe_paymentsheet_payment_method_sofort
        )
        assertThat((selection as? PaymentSelection.New.GenericPaymentMethod)?.iconResource).isEqualTo(
            R.drawable.stripe_ic_paymentsheet_pm_klarna
        )
    }
}
