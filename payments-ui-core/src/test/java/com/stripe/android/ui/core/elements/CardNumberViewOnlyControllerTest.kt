package com.stripe.android.ui.core.elements

import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.CardBrand
import com.stripe.android.ui.core.forms.FormFieldEntry
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardNumberViewOnlyControllerTest {

    private val cardNumber = "123"
    private val cardNumberController = CardNumberViewOnlyController(
        CardNumberConfig(),
        mapOf(
            IdentifierSpec.CardNumber to cardNumber,
            IdentifierSpec.CardBrand to CardBrand.Visa.code
        )
    )

    @Test
    fun `Verify no error`() {
        val errorFlowValues = mutableListOf<FieldError?>()
        cardNumberController.error.asLiveData()
            .observeForever {
                errorFlowValues.add(it)
            }

        assertThat(errorFlowValues).containsExactly(null)
    }

    @Test
    fun `Verify value does not change`() {
        var formFieldValue: FormFieldEntry? = null
        cardNumberController.formFieldValue.asLiveData()
            .observeForever {
                formFieldValue = it
            }

        assertThat(formFieldValue).isEqualTo(FormFieldEntry(cardNumber, true))

        cardNumberController.onValueChange("012")
        assertThat(formFieldValue).isEqualTo(FormFieldEntry(cardNumber, true))
    }

    @Test
    fun `Verify card brand is emitted`() {
        var cardBrand: CardBrand? = null
        cardNumberController.cardBrandFlow.asLiveData()
            .observeForever {
                cardBrand = it
            }

        assertThat(cardBrand).isEqualTo(CardBrand.Visa)
    }
}
