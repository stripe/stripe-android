package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardDetailsElementTest {

    private val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeDefaultTheme)

    @Test
    fun `test form field values returned and expiration date parsing`() {
        val cardController = CardDetailsController(context)
        val cardDetailsElement = CardDetailsElement(
            IdentifierSpec.Generic("card_details"),
            context,
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
                IdentifierSpec.Generic("card[cvc]") to FormFieldEntry("321", true),
                IdentifierSpec.CardBrand to FormFieldEntry("visa", true),
                IdentifierSpec.Generic("card[exp_month]") to FormFieldEntry("1", true),
                IdentifierSpec.Generic("card[exp_year]") to FormFieldEntry("30", true),
            )
        )
    }
}
