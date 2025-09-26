package com.stripe.android.paymentsheet.verticalmode

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.core.utils.FeatureFlags
import com.stripe.android.isInstanceOf
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethodFixtures
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
import com.stripe.android.paymentsheet.utils.errorTest
import com.stripe.android.paymentsheet.verticalmode.VerticalModeFormInteractor.ViewAction
import com.stripe.android.testing.CoroutineTestRule
import com.stripe.android.ui.core.elements.CardDetailsSectionController
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.ui.core.elements.SetAsDefaultPaymentMethodElement
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeLinkConfigurationCoordinator
import com.stripe.android.utils.NullCardAccountRangeRepositoryFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import com.stripe.android.uicore.R as UiCoreR

internal class DefaultVerticalModeFormInteractorTest {

    @get:Rule
    val rule: TestRule = CoroutineTestRule()

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
        runScenario(
            selectedPaymentMethodCode = "randomTestValue",
        ) {
            interactor.handleViewAction(ViewAction.FieldInteraction)
            assertThat(reportFieldInteractionTurbine.awaitItem()).isEqualTo("randomTestValue")
        }
    }

    @Test
    fun `handleViewAction FormFieldValuesChanged calls onFormFieldValuesChanged`() {
        val expectedFormValues = FormFieldValues(
            fieldValuePairs = emptyMap(),
            userRequestedReuse = PaymentSelection.CustomerRequestedSave.NoRequest,
        )
        runScenario(
            selectedPaymentMethodCode = "randomTestValue",
        ) {
            interactor.handleViewAction(ViewAction.FormFieldValuesChanged(expectedFormValues))
            onFormFieldValuesChangedTurbine.awaitItem().run {
                assertThat(first).isEqualTo(expectedFormValues)
                assertThat(second).isEqualTo("randomTestValue")
            }
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

    @Test
    fun `on validation requested true, form elements should be in validation state`() {
        runScenario(
            selectedPaymentMethodCode = "card",
            formElements = listOf(
                SectionElement.wrap(
                    listOf(
                        SimpleTextElement(
                            IdentifierSpec.Name,
                            SimpleTextFieldController(
                                textFieldConfig = SimpleTextFieldConfig(
                                    label = resolvableString("")
                                )
                            ),
                        ),
                        EmailElement()
                    )
                )
            ),
        ) {
            interactor.state.test {
                val state = awaitItem()

                val sectionElement = state.formUiElements[0] as SectionElement

                sectionElement.fields.errorTest(identifierSpec = IdentifierSpec.Name, error = null)
                sectionElement.fields.errorTest(identifierSpec = IdentifierSpec.Email, error = null)

                validationRequestedSource.emit(Unit)

                val nextState = awaitItem()

                val nextSectionElement = nextState.formUiElements[0] as SectionElement

                nextSectionElement.fields.errorTest(
                    identifierSpec = IdentifierSpec.Name,
                    error = FieldError(UiCoreR.string.stripe_blank_and_required),
                )
                nextSectionElement.fields.errorTest(
                    identifierSpec = IdentifierSpec.Email,
                    error = FieldError(UiCoreR.string.stripe_blank_and_required),
                )
            }
        }
    }

    @Test
    fun `automaticallyLaunchCardScan when card form and no create params`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)
        testAutomaticallyLaunchCardScan(
            selectedPaymentMethodCode = "card",
            paymentSelection = null,
        ) { elements ->
            assertThat(elements[0].controller).isInstanceOf<CardDetailsSectionController>()

            val controller = elements[0].controller as CardDetailsSectionController

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isTrue()
        }
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

    @Test
    fun `do not automaticallyLaunchCardScan when card form and with paymentSelection`() {
        FeatureFlags.cardScanGooglePayMigration.setEnabled(true)
        testAutomaticallyLaunchCardScan(
            selectedPaymentMethodCode = "card",
            paymentSelection = PaymentMethodFixtures.CARD_PAYMENT_SELECTION,
        ) { elements ->
            assertThat(elements[0].controller).isInstanceOf<CardDetailsSectionController>()

            val controller = elements[0].controller as CardDetailsSectionController

            assertThat(controller.shouldAutomaticallyLaunchCardScan()).isFalse()
        }
        FeatureFlags.cardScanGooglePayMigration.setEnabled(false)
    }

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
            selectedPaymentMethodCode = "",
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

        val formElements = setAsDefaultInteractor.state.value.formUiElements

        val saveForFutureUseElement = formElements.firstOrNull {
            it.identifier == IdentifierSpec.SaveForFutureUse
        } as? SaveForFutureUseElement
        val setAsDefaultElement = formElements.firstOrNull {
            it.identifier == IdentifierSpec.SetAsDefaultPaymentMethod
        } as? SetAsDefaultPaymentMethodElement

        block(saveForFutureUseElement, setAsDefaultElement)
    }

    private fun testAutomaticallyLaunchCardScan(
        selectedPaymentMethodCode: String,
        paymentSelection: PaymentSelection?,
        block: (List<FormElement>) -> Unit
    ) {
        val paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            openCardScanAutomatically = true,
        )
        val selectionHolder = EmbeddedSelectionHolder(SavedStateHandle())
        selectionHolder.set(paymentSelection)
        val stateHolder = DefaultFormActivityStateHelper(
            paymentMethodMetadata = paymentMethodMetadata,
            selectionHolder = selectionHolder,
            configuration = EmbeddedConfirmationStateFixtures.defaultStateWithOpenCardScanAutomatically().configuration,
            coroutineScope = TestScope(UnconfinedTestDispatcher()),
            onClickDelegate = OnClickDelegateOverrideImpl(),
            eventReporter = FakeEventReporter()
        )
        val formHelperFactory = EmbeddedFormHelperFactory(
            linkConfigurationCoordinator = FakeLinkConfigurationCoordinator(),
            embeddedSelectionHolder = selectionHolder,
            cardAccountRangeRepositoryFactory = NullCardAccountRangeRepositoryFactory,
            savedStateHandle = SavedStateHandle(),
            selectedPaymentMethodCode = selectedPaymentMethodCode,
        )
        val eventReporter = FakeEventReporter()
        val setAsDefaultInteractor = EmbeddedFormInteractorFactory(
            paymentMethodMetadata = paymentMethodMetadata,
            paymentMethodCode = selectedPaymentMethodCode,
            hasSavedPaymentMethods = false,
            embeddedSelectionHolder = selectionHolder,
            embeddedFormHelperFactory = formHelperFactory,
            viewModelScope = TestScope(UnconfinedTestDispatcher()),
            formActivityStateHelper = stateHolder,
            eventReporter = eventReporter
        ).create()
        block(setAsDefaultInteractor.state.value.formUiElements)
    }

    private fun runScenario(
        selectedPaymentMethodCode: String,
        formElements: List<FormElement> = emptyList(),
        testBlock: suspend TestParams.() -> Unit,
    ) {
        val formArguments = mock<FormArguments>()
        val usBankAccountArguments = mock<USBankAccountFormArguments>()
        val processing: MutableStateFlow<Boolean> = MutableStateFlow(false)
        val validationRequested = MutableSharedFlow<Unit>()

        val onFormFieldValuesChangedTurbine = Turbine<Pair<FormFieldValues?, String>>()
        val reportFieldInteractionTurbine = Turbine<String>()

        val interactor = DefaultVerticalModeFormInteractor(
            selectedPaymentMethodCode = selectedPaymentMethodCode,
            formArguments = formArguments,
            formElements = formElements,
            onFormFieldValuesChanged = { formValues: FormFieldValues?, selectedPaymentMethodCode: String ->
                onFormFieldValuesChangedTurbine.add(Pair(formValues, selectedPaymentMethodCode))
            },
            usBankAccountArguments = usBankAccountArguments,
            reportFieldInteraction = {
                reportFieldInteractionTurbine.add(it)
            },
            headerInformation = null,
            isLiveMode = true,
            processing = processing,
            validationRequested = validationRequested,
            coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
            paymentMethodIncentive = stateFlowOf(null),
            uiContext = UnconfinedTestDispatcher(),
        )

        TestParams(
            interactor = interactor,
            processingSource = processing,
            validationRequestedSource = validationRequested,
            onFormFieldValuesChangedTurbine = onFormFieldValuesChangedTurbine,
            reportFieldInteractionTurbine = reportFieldInteractionTurbine,
        ).apply {
            runTest {
                testBlock()
            }
        }

        verifyNoMoreInteractions(formArguments, usBankAccountArguments)
        onFormFieldValuesChangedTurbine.ensureAllEventsConsumed()
        reportFieldInteractionTurbine.ensureAllEventsConsumed()
    }

    private class TestParams(
        val interactor: DefaultVerticalModeFormInteractor,
        val processingSource: MutableStateFlow<Boolean>,
        val validationRequestedSource: MutableSharedFlow<Unit>,
        val onFormFieldValuesChangedTurbine: ReceiveTurbine<Pair<FormFieldValues?, String>>,
        val reportFieldInteractionTurbine: ReceiveTurbine<String>,
    )
}
