package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor.ViewAction
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions

internal class DefaultVerticalModeFormInteractorTest {
    @Test
    fun `state is updated when processing emits`() = runScenario(selectedPaymentMethodCode = "card") {
        interactor.state.test {
            assertThat(awaitItem().isProcessing).isFalse()

            processingSource.value = true
            assertThat(awaitItem().isProcessing).isTrue()
        }
    }

    @Test
    fun `handleViewAction FieldInteraction calls reportFieldInteraction`() {
        var fieldInteractionValue: String? = null
        runScenario(
            selectedPaymentMethodCode = "randomTestValue",
            reportFieldInteraction = { fieldInteractionValue = it },
        ) {
            interactor.handleViewAction(ViewAction.FieldInteraction)
            assertThat(fieldInteractionValue).isEqualTo("randomTestValue")
        }
    }

    @Test
    fun `handleViewAction FormFieldValuesChanged calls onFormFieldValuesChanged`() {
        var onFormFieldValuesChangedCalled = false
        val expectedFormValues = FormFieldValues(
            fieldValuePairs = emptyMap(),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        runScenario(
            selectedPaymentMethodCode = "randomTestValue",
            onFormFieldValuesChanged = { formValues, selectedPaymentMethodCode ->
                assertThat(formValues).isEqualTo(expectedFormValues)
                assertThat(selectedPaymentMethodCode).isEqualTo("randomTestValue")
                onFormFieldValuesChangedCalled = true
            },
        ) {
            interactor.handleViewAction(ViewAction.FormFieldValuesChanged(expectedFormValues))
            assertThat(onFormFieldValuesChangedCalled).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        selectedPaymentMethodCode: String,
        onFormFieldValuesChanged: (formValues: FormFieldValues?, selectedPaymentMethodCode: String) -> Unit = { _, _ ->
            notImplemented()
        },
        reportFieldInteraction: (String) -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit,
    ) {
        val formArguments = mock<FormArguments>()
        val usBankAccountArguments = mock<USBankAccountFormArguments>()
        val processing: MutableStateFlow<Boolean> = MutableStateFlow(false)

        val interactor = DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            formArguments = formArguments,
            formElements = emptyList(),
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            usBankAccountArguments = usBankAccountArguments,
            reportFieldInteraction = reportFieldInteraction,
            headerInformation = null,
            isLiveMode = true,
            processing = processing,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            paymentMethodIncentive = stateFlowOf(null),
        )

        TestParams(
            interactor = interactor,
            processingSource = processing,
        ).apply {
            runTest {
                testBlock()
            }
        }

        verifyNoMoreInteractions(formArguments, usBankAccountArguments)
    }

    private class TestParams(
        val interactor: DefaultVerticalModeFormInteractor,
        val processingSource: MutableStateFlow<Boolean>,
    )
}
