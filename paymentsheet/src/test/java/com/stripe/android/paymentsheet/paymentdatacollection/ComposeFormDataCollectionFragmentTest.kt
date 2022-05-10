package com.stripe.android.paymentsheet.paymentdatacollection

import androidx.core.os.bundleOf
import androidx.fragment.app.testing.launchFragment
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.paymentsheet.R
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SupportedPaymentMethod
import com.stripe.android.paymentsheet.paymentdatacollection.ComposeFormDataCollectionFragment.Companion.EXTRA_CONFIG
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ComposeFormDataCollectionFragmentTest {

    @Test
    fun `card payment method selection has the fields from formFieldValues`() {
        mockFragment {
            val formFieldValues = FormFieldValues(
                fieldValuePairs = mapOf(
                    IdentifierSpec.SaveForFutureUse to FormFieldEntry("true", true),
                    IdentifierSpec.CardNumber to FormFieldEntry("4242424242421234", true),
                    IdentifierSpec.CardBrand to FormFieldEntry(CardBrand.Visa.code, true)
                ),
                showsMandate = false,
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
            val selection = it.transformToPaymentSelection(
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
    }

    @Test
    fun `payment method selection has the fields from formFieldValues`() {
        mockFragment {
            val formFieldValues = FormFieldValues(
                fieldValuePairs = mapOf(
                    IdentifierSpec.SaveForFutureUse to FormFieldEntry("true", true)
                ),
                showsMandate = false,
                userRequestedReuse = PaymentSelection.CustomerRequestedSave.RequestReuse
            )
            val selection = it.transformToPaymentSelection(
                formFieldValues,
                SupportedPaymentMethod.Sofort
            )
            assertThat(selection?.customerRequestedSave).isEqualTo(
                PaymentSelection.CustomerRequestedSave.RequestReuse
            )
            assertThat((selection as? PaymentSelection.New.GenericPaymentMethod)?.labelResource).isEqualTo(
                "Sofort"
            )
            assertThat((selection as? PaymentSelection.New.GenericPaymentMethod)?.iconResource).isEqualTo(
                R.drawable.stripe_ic_paymentsheet_pm_klarna
            )
        }
    }

    private fun mockFragment(operations: (ComposeFormDataCollectionFragment) -> Unit) {
        val args = mock<FormFragmentArguments>()
        whenever(args.paymentMethod).thenReturn(mock())
        whenever(args.paymentMethod.formSpec).thenReturn(mock())
        launchFragment<ComposeFormDataCollectionFragment>(
            bundleOf(
                EXTRA_CONFIG to args
            )
        ).onFragment {
            operations(it)
        }
    }
}
