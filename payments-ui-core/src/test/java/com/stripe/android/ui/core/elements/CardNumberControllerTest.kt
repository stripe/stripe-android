package com.stripe.android.ui.core.elements

import androidx.appcompat.view.ContextThemeWrapper
import androidx.lifecycle.asLiveData
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.stripe.android.ui.core.R
import com.stripe.android.ui.core.forms.FormFieldEntry
import com.stripe.android.utils.TestUtils.idleLooper
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class CardNumberControllerTest {
    private val cardNumberController = CardNumberController(
        CardNumberConfig(), ContextThemeWrapper(ApplicationProvider.getApplicationContext(), R.style.StripeDefaultTheme)
    )

    @Test
    fun `When invalid card number verify visible error`() {

        val errorFlowValues = mutableListOf<FieldError?>()
        cardNumberController.error.asLiveData()
            .observeForever {
                errorFlowValues.add(it)
            }

        cardNumberController.onValueChange("012")
        idleLooper()

        assertThat(errorFlowValues[errorFlowValues.size - 1]?.errorMessage)
            .isEqualTo(R.string.invalid_card_number)
    }

    @Test
    fun `Verify get the form field value correctly`() {
        val formFieldValuesFlow = mutableListOf<FormFieldEntry?>()
        cardNumberController.formFieldValue.asLiveData()
            .observeForever {
                formFieldValuesFlow.add(it)
            }

        cardNumberController.onValueChange("4242")
        idleLooper()

        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.isComplete)
            .isFalse()
        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.value)
            .isEqualTo("4242")

        cardNumberController.onValueChange("4242424242424242")
        idleLooper()

        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.isComplete)
            .isTrue()
        assertThat(formFieldValuesFlow[formFieldValuesFlow.size - 1]?.value)
            .isEqualTo("4242424242424242")
    }

    @Test
    fun `Verify error is visible based on the focus`() {
        // incomplete
        val visibleErrorFlow = mutableListOf<Boolean>()
        cardNumberController.visibleError.asLiveData()
            .observeForever {
                visibleErrorFlow.add(it)
            }

        cardNumberController.onFocusChange(true)
        cardNumberController.onValueChange("4242")
        idleLooper()

        assertThat(visibleErrorFlow[visibleErrorFlow.size - 1])
            .isFalse()

        cardNumberController.onFocusChange(false)
        idleLooper()

        assertThat(visibleErrorFlow[visibleErrorFlow.size - 1])
            .isTrue()
    }
}
