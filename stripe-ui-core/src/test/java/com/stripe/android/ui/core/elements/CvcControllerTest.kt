package com.stripe.android.ui.core.elements

import androidx.lifecycle.asLiveData
import com.google.common.truth.Truth
import com.stripe.android.core.model.CardBrand
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CvcControllerTest {
    private val cardBrandFlow = MutableStateFlow(CardBrand.Visa)
    private val cvcController = CvcController(
        CvcConfig(),
        cardBrandFlow
    )

    @Test
    fun `When invalid card number verify visible error`() {
        val errorFlowValues = mutableListOf<FieldError?>()
        cvcController.error.asLiveData()
            .observeForever {
                errorFlowValues.add(it)
            }

        cvcController.onValueChange("12")
        idleLooper()

        assertThat(errorFlowValues[errorFlowValues.size - 1]?.errorMessage)
            .isEqualTo(R.string.invalid_cvc)
    }

    @Test
    fun `Verify get the form field value correctly`() {
        val formFieldValuesFlow = mutableListOf<FormFieldEntry?>()
        cvcController.formFieldValue.asLiveData()
            .observeForever {
                formFieldValuesFlow.add(it)
            }
        cvcController.onValueChange("13")
        idleLooper()

        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.isComplete)
            .isFalse()
        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.value)
            .isEqualTo("13")

        cvcController.onValueChange("123")
        idleLooper()

        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.isComplete)
            .isTrue()
        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.value)
            .isEqualTo("123")
    }

    @Test
    fun `Verify error is visible based on the focus`() {
        // incomplete
        val visibleErrorFlow = mutableListOf<Boolean>()
        cvcController.visibleError.asLiveData()
            .observeForever {
                visibleErrorFlow.add(it)
            }

        cvcController.onFocusChange(true)
        cvcController.onValueChange("12")
        idleLooper()

        Truth.assertThat(visibleErrorFlow[visibleErrorFlow.size - 1])
            .isFalse()

        cvcController.onFocusChange(false)
        idleLooper()

        Truth.assertThat(visibleErrorFlow[visibleErrorFlow.size - 1])
            .isTrue()
    }
}
