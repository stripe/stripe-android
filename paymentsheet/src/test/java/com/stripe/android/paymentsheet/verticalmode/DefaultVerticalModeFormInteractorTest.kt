package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.paymentelement.ExperimentalEmbeddedPaymentElementApi
import com.stripe.android.paymentelement.embedded.EmbeddedFormHelperFactory
import com.stripe.android.paymentelement.embedded.EmbeddedSelectionHolder
import com.stripe.android.paymentelement.embedded.content.EmbeddedConfirmationStateFixtures
import com.stripe.android.paymentelement.embedded.form.DefaultFormActivityStateHelper
import com.stripe.android.paymentelement.embedded.form.EmbeddedFormInteractorFactory
import com.stripe.android.paymentelement.embedded.form.OnClickDelegateOverrideImpl
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor.ViewAction
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions

internal class DefaultVerticalModeFormInteractorTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val rule: TestRule = CoroutineTestRule(testDispatcher)

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

    @Test
    fun `SetAsDefault shown when SaveForFutureUseChecked when hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = true,
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement!!.shouldShowElementFlow.value).isTrue()
        }
    }

    @Test
    fun `SetAsDefault field default false when SaveForFutureUseChecked when hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = true
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller
            val setAsDefaultController = setAsDefaultPaymentMethodElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultController.fieldValue.value.toBoolean()).isFalse()
        }
    }

    @Test
    fun `SetAsDefault hidden when SaveForFutureUseChecked when not hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = false
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement!!.shouldShowElementFlow.value).isFalse()
        }
    }

    @Test
    fun `SetAsDefault field true when SaveForFutureUseChecked when not hasSavedPaymentMethods`() {
        testSetAsDefaultElements(
            hasSavedPaymentMethods = false
        ) { saveForFutureUseElement, setAsDefaultPaymentMethodElement ->
            assertThat(saveForFutureUseElement).isNotNull()
            assertThat(setAsDefaultPaymentMethodElement).isNotNull()

            val saveForFutureUseController = saveForFutureUseElement!!.controller

            saveForFutureUseController.onValueChange(true)
            assertThat(setAsDefaultPaymentMethodElement!!.controller.fieldValue.value.toBoolean()).isTrue()
        }
    }

    @OptIn(ExperimentalEmbeddedPaymentElementApi::class)
    private fun testSetAsDefaultElements(
        hasSavedPaymentMethods: Boolean,
        block: (SaveForFutureUseElement?, SetAsDefaultPaymentMethodElement?) -> Unit
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            billingDetailsCollectionConfiguration = PaymentSheet.BillingDetailsCollectionConfiguration(
                address = PaymentSheet.BillingDetailsCollectionConfiguration.AddressCollectionMode.Full
            ),
            paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Enabled,
            hasCustomerConfiguration = true,
            isPaymentMethodSetAsDefaultEnabled = true,
        )
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        val stateHolder = DefaultFormActivityStateHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultState().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl(),
            eventReporter = FakeEventReporter()
        )
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
        )
        val eventReporter = FakeEventReporter()
        val setAsDefaultInteractor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethodCode = "card",
            hasSavedPaymentMethods = hasSavedPaymentMethods,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = TestScope(UnconfinedTestDispatcher()),
            formActivityStateHelper = stateHolder,
            eventReporter = eventReporter
        ).create()

        val formElements = setAsDefaultInteractor.state.value.formElements

        val saveForFutureUseElement = formElements.firstOrNull {
            it.identifier == IdentifierSpec.SaveForFutureUse
        } as? SaveForFutureUseElement
        val setAsDefaultElement = formElements.firstOrNull {
            it.identifier == IdentifierSpec.SetAsDefaultPaymentMethod
        } as? SetAsDefaultPaymentMethodElement

        block(saveForFutureUseElement, setAsDefaultElement)
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
