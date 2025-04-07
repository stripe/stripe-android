package com.stripe.android.paymentsheet.ui

import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.testing.createComposeCleanupRule
import com.stripe.android.uicore.utils.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class PaymentMethodFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val composeCleanupRule = createComposeCleanupRule()

    @Test
    fun `Changing payment method emits only form values of newly selected payment method`() {
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

        // Changing the payment method from card to PayPal.
        paymentMethodCodeFlow.value = PaymentMethod.Type.PayPal.code

        // PayPalValues should only be associated with the PayPal payment method code, not card.
        val payPalValues = FormFieldValues(
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        completeFormValuesFlow.value = payPalValues

        assertThat(emissions).containsExactly(null, payPalValues)

        paymentMethodCodeFlow.value = PaymentMethod.Type.Card.code
        completeFormValuesFlow.value = null

        assertThat(emissions).containsExactly(null, payPalValues, null)
    }

    @Test
    fun `Changing completeFormValues emits new FormFieldValues`() {
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

        val completeValues = FormFieldValues(
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        completeFormValuesFlow.value = completeValues

        assertThat(emissions).containsExactly(null, completeValues)
    }
}
