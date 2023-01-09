package com.stripe.android.paymentsheet.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PaymentMethodFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun foo() {
        val paymentMethodCodeFlow = MutableStateFlow(PaymentMethod.Type.Card.code)
        val completeFormValuesFlow = MutableStateFlow<FormFieldValues?>(null)

        val emissions = mutableListOf<FormFieldValues?>()

        composeTestRule.setContent {
            val code by paymentMethodCodeFlow.collectAsState()

            PaymentMethodForm(
                paymentMethodCode = code,
                enabled = true,
                completeFormValues = completeFormValuesFlow,
                onFormFieldValuesChanged = {
                    emissions.add(it)
                },
                hiddenIdentifiers = emptySet(),
                elements = emptyList(),
                lastTextFieldIdentifier = null,
            )
        }

        assertThat(emissions).containsExactly(null)

        // TODO Comment
        paymentMethodCodeFlow.value = PaymentMethod.Type.PayPal.code

        // TODO Comment
        val paypalValues = FormFieldValues(
            showsMandate = false,
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        completeFormValuesFlow.value = paypalValues

        assertThat(emissions).containsExactly(null, paypalValues)

        paymentMethodCodeFlow.value = PaymentMethod.Type.Card.code
        completeFormValuesFlow.value = null

        assertThat(emissions).containsExactly(null, paypalValues, null)
    }
}
