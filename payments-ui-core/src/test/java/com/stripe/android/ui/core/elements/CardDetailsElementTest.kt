package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsElementTest {

    private val context =
        ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeDefaultTheme)

    @Test
    fun `test form field values returned and expiration date parsing`() {
        val cardController = CardDetailsController(context, emptyMap())
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
            initialValues = emptyMap(),
            viewOnlyFields = emptySet(),
            cardController
        )

        val flowValues = mutableListOf<List<Pair<IdentifierSpec, FormFieldEntry>>>()
        cardDetailsElement.getFormFieldValueFlow().asLiveData()
            .observeForever {
                flowValues.add(it)
            }

        cardDetailsElement.controller.numberElement.controller.onValueChange("4242424242424242")
        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        idleLooper()

        Truth.assertThat(flowValues[flowValues.size - 1]).isEqualTo(
            listOf(
                IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.CardExpMonth to FormFieldEntry("1", true),
                IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
            )
        )
    }

    @Test
    fun `test view only form field values returned and expiration date parsing`() {
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
            initialValues = mapOf(
                IdentifierSpec.CardNumber to "4242424242424242",
                IdentifierSpec.CardBrand to CardBrand.Visa.code
            ),
            viewOnlyFields = setOf(IdentifierSpec.CardNumber)
        )

        val flowValues = mutableListOf<List<Pair<IdentifierSpec, FormFieldEntry>>>()
        cardDetailsElement.getFormFieldValueFlow().asLiveData()
            .observeForever {
                flowValues.add(it)
            }

        cardDetailsElement.controller.cvcElement.controller.onValueChange("321")
        cardDetailsElement.controller.expirationDateElement.controller.onValueChange("130")

        idleLooper()

        Truth.assertThat(flowValues[flowValues.size - 1]).isEqualTo(
            listOf(
                IdentifierSpec.CardNumber to FormFieldEntry("4242424242424242", true),
                IdentifierSpec.CardCvc to FormFieldEntry("321", true),
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.CardExpMonth to FormFieldEntry("1", true),
                IdentifierSpec.CardExpYear to FormFieldEntry("2030", true)
            )
        )
    }
}
