package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.link.LinkConfigurationCoordinator
import com.stripe.android.link.ui.inline.InlineSignupViewState
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor.ViewAction
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
    fun `state is updated when linkSignupMode emits`() = runScenario(selectedPaymentMethodCode = "card") {
        interactor.state.test {
            assertThat(awaitItem().linkSignupMode).isNull()

            linkSignupModeSource.value = LinkSignupMode.InsteadOfSaveForFutureUse
            assertThat(awaitItem().linkSignupMode).isEqualTo(LinkSignupMode.InsteadOfSaveForFutureUse)
        }
    }

    @Test
    fun `linkSignupMode is always null for non card LPM`() = runScenario(selectedPaymentMethodCode = "cashapp") {
        interactor.state.test {
            assertThat(awaitItem().linkSignupMode).isNull()

            linkSignupModeSource.value = LinkSignupMode.InsteadOfSaveForFutureUse
            expectNoEvents()
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

    @Test
    fun `handleViewAction LinkSignupStateChanged calls onLinkInlineStateUpdated`() {
        var linkInlineState: InlineSignupViewState? = null
        runScenario(
            selectedPaymentMethodCode = "card",
            onLinkInlineStateUpdated = { linkInlineState = it },
        ) {
            val expectedViewState = mock<InlineSignupViewState>()
            interactor.handleViewAction(ViewAction.LinkSignupStateChanged(expectedViewState))
            assertThat(linkInlineState).isEqualTo(expectedViewState)
        }
    }

    @Test
    fun `canGoBack calls delegate`() {
        var canGoBack = false
        runScenario(
            selectedPaymentMethodCode = "card",
            canGoBackDelegate = { canGoBack },
        ) {
            assertThat(interactor.canGoBack()).isFalse()
            canGoBack = true
            assertThat(interactor.canGoBack()).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        selectedPaymentMethodCode: String,
        onLinkInlineStateUpdated: (InlineSignupViewState) -> Unit = { notImplemented() },
        onFormFieldValuesChanged: (formValues: FormFieldValues?, selectedPaymentMethodCode: String) -> Unit = { _, _ ->
            notImplemented()
        },
        reportFieldInteraction: (String) -> Unit = { notImplemented() },
        canGoBackDelegate: () -> Boolean = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit,
    ) {
        val linkConfigurationCoordinator = mock<LinkConfigurationCoordinator>()
        val linkSignupMode: MutableStateFlow<LinkSignupMode?> = MutableStateFlow(null)
        val formArguments = mock<FormArguments>()
        val usBankAccountArguments = mock<USBankAccountFormArguments>()
        val processing: MutableStateFlow<Boolean> = MutableStateFlow(false)

        val interactor = DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            linkConfigurationCoordinator = linkConfigurationCoordinator,
            onLinkInlineStateUpdated = onLinkInlineStateUpdated,
            linkSignupMode = linkSignupMode,
            formArguments = formArguments,
            formElements = emptyList(),
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            usBankAccountArguments = usBankAccountArguments,
            reportFieldInteraction = reportFieldInteraction,
            headerInformation = null,
            isLiveMode = true,
            canGoBackDelegate = canGoBackDelegate,
            processing = processing,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
        )

        TestParams(
            interactor = interactor,
            linkSignupModeSource = linkSignupMode,
            processingSource = processing,
        ).apply {
            runTest {
                testBlock()
            }
        }

        verifyNoMoreInteractions(linkConfigurationCoordinator, formArguments, usBankAccountArguments)
    }

    private class TestParams(
        val interactor: DefaultVerticalModeFormInteractor,
        val linkSignupModeSource: MutableStateFlow<LinkSignupMode?>,
        val processingSource: MutableStateFlow<Boolean>,
    )
}
