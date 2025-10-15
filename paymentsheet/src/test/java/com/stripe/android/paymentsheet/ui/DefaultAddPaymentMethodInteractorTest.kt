package com.stripe.android.paymentsheet.ui

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.MANY_ITEMS_ONE_PARTIALLY_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_HIDDEN
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.ONE_ITEM
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.ONE_ITEM_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.THREE_ITEMS
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.THREE_ITEMS_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.TWO_ITEMS
import com.stripe.android.paymentsheet.ui.AddPaymentMethodInitialVisibilityTrackerDataFixtures.TWO_ITEMS_EXPECTED_VISIBLE
import com.stripe.android.paymentsheet.utils.errorTest
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.EmailElement
import com.stripe.android.uicore.elements.FieldError
import com.stripe.android.uicore.elements.FormElement
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.elements.SectionElement
import com.stripe.android.uicore.elements.SimpleTextElement
import com.stripe.android.uicore.elements.SimpleTextFieldConfig
import com.stripe.android.uicore.elements.SimpleTextFieldController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock
import kotlin.test.Test
import com.stripe.android.uicore.R as UiCoreR

class DefaultAddPaymentMethodInteractorTest {
    @Test
    fun handleViewAction_ReportFieldInteraction_reportsFieldInteraction() {
        runScenario {
            val expectedCode = PaymentMethod.Type.CashAppPay.code
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction(expectedCode)
            )

            assertThat(reportFieldInteractionTurbine.awaitItem()).isEqualTo(expectedCode)
        }
    }

    @Test
    fun handleViewAction_OnFormFieldValuesChanged_updatesFormFields() {
        runScenario {
            val expectedCode = PaymentMethod.Type.CashAppPay.code
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(null, expectedCode)
            )

            assertThat(onFormFieldValuesChangedTurbine.awaitItem().second).isEqualTo(expectedCode)
        }
    }

    @Test
    fun handleViewAction_OnPaymentMethodSelected_selectsPaymentMethod() {
        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
        ) {
            val expectedCode = PaymentMethod.Type.CashAppPay.code
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(expectedCode)
            )

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(reportPaymentMethodTypeSelectedTurbine.awaitItem()).isEqualTo(expectedCode)
            assertThat(clearErrorMessagesTurbine.awaitItem()).isNotNull()
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(expectedCode)
                }
            }
        }
    }

    @Test
    fun handleViewAction_OnPaymentMethodSelected_withoutNewPaymentMethod_doesntReportSelection() {
        val expectedCode = PaymentMethod.Type.CashAppPay.code
        runScenario(
            initiallySelectedPaymentMethodType = expectedCode,
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(expectedCode)
            )

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(expectedCode)
                }
            }
        }
    }

    @Test
    fun initialState_isCorrect() {
        val expectedSelectedPaymentMethodCode = PaymentMethod.Type.CashAppPay.code
        val expectedSelection = PaymentSelection.Saved(
            paymentMethod = PaymentMethodFactory.usBankAccount(),
        )
        val expectedProcessing = false

        var formArgumentsCreatedForCode: PaymentMethodCode? = null
        fun createFormArguments(code: PaymentMethodCode): FormArguments {
            formArgumentsCreatedForCode = code
            return mock()
        }

        var formElementsCreatedForCode: PaymentMethodCode? = null
        fun formElementsForCode(code: PaymentMethodCode): List<FormElement> {
            formElementsCreatedForCode = code
            return emptyList()
        }

        runScenario(
            initiallySelectedPaymentMethodType = expectedSelectedPaymentMethodCode,
            selection = MutableStateFlow(expectedSelection),
            processing = MutableStateFlow(expectedProcessing),
            createFormArguments = ::createFormArguments,
            formElementsForCode = ::formElementsForCode,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(expectedSelectedPaymentMethodCode)
                    assertThat(paymentSelection).isEqualTo(expectedSelection)
                    assertThat(processing).isEqualTo(expectedProcessing)

                    assertThat(formArgumentsCreatedForCode).isEqualTo(expectedSelectedPaymentMethodCode)
                    assertThat(formElementsCreatedForCode).isEqualTo(expectedSelectedPaymentMethodCode)
                }
            }
        }
    }

    @Test
    fun updatingSelectedPaymentMethod_updatesFormInfo() {
        var formArgumentsCreatedForCode: PaymentMethodCode? = null
        fun createFormArguments(code: PaymentMethodCode): FormArguments {
            formArgumentsCreatedForCode = code
            return mock()
        }

        var formElementsCreatedForCode: PaymentMethodCode? = null
        fun formElementsForCode(code: PaymentMethodCode): List<FormElement> {
            formElementsCreatedForCode = code
            return emptyList()
        }

        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
            createFormArguments = ::createFormArguments,
            formElementsForCode = ::formElementsForCode,
        ) {
            val newPaymentMethodCode = PaymentMethod.Type.CashAppPay.code
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(
                    newPaymentMethodCode
                )
            )

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                assertThat(clearErrorMessagesTurbine.awaitItem()).isNotNull()
                assertThat(reportPaymentMethodTypeSelectedTurbine.awaitItem()).isEqualTo(newPaymentMethodCode)

                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(newPaymentMethodCode)

                    assertThat(formArgumentsCreatedForCode).isEqualTo(newPaymentMethodCode)
                    assertThat(formElementsCreatedForCode).isEqualTo(newPaymentMethodCode)
                }
            }
        }
    }

    @Test
    fun changingSelectedPaymentMethod_clearsErrorMessages() {
        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(
                    PaymentMethod.Type.CashAppPay.code,
                )
            )
            assertThat(reportPaymentMethodTypeSelectedTurbine.awaitItem()).isEqualTo("cashapp")

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(clearErrorMessagesTurbine.awaitItem()).isNotNull()
        }
    }

    @Test
    fun `on validation requested true, form elements should be in validation state`() {
        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
            dispatcher = UnconfinedTestDispatcher(),
            formElementsForCode = {
                listOf(
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
                            EmailElement(),
                        )
                    )
                )
            },
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

            assertThat(clearErrorMessagesTurbine.awaitItem()).isNotNull()
        }
    }
    @Test
    fun updatingVisibilityTrackerWith1ItemEmitsRightEvent() = runScenario {
        interactor.handleViewAction(
            viewAction = AddPaymentMethodInteractor.ViewAction.UpdatePaymentMethodVisibility(
                initialVisibilityTrackerData = ONE_ITEM,
            )
        )

        val visibilityItem = initialVisibilityTrackerTurbine.awaitItem()

        assertThat(visibilityItem.first).isEqualTo(ONE_ITEM_EXPECTED_VISIBLE)

        assertThat(visibilityItem.second).isEqualTo(emptyList<String>())
    }

    @Test
    fun updatingVisibilityTrackerWith2ItemsEmitsRightEvent() = runScenario {
        interactor.handleViewAction(
            viewAction = AddPaymentMethodInteractor.ViewAction.UpdatePaymentMethodVisibility(
                initialVisibilityTrackerData = TWO_ITEMS,
            )
        )

        val visibilityItem = initialVisibilityTrackerTurbine.awaitItem()

        assertThat(visibilityItem.first).isEqualTo(TWO_ITEMS_EXPECTED_VISIBLE)

        assertThat(visibilityItem.second).isEqualTo(emptyList<String>())
    }

    @Test
    fun updatingVisibilityTrackerWith3ItemsEmitsRightEvent() = runScenario {
        interactor.handleViewAction(
            viewAction = AddPaymentMethodInteractor.ViewAction.UpdatePaymentMethodVisibility(
                initialVisibilityTrackerData = THREE_ITEMS,
            )
        )

        val visibilityItem = initialVisibilityTrackerTurbine.awaitItem()

        assertThat(visibilityItem.first).isEqualTo(THREE_ITEMS_EXPECTED_VISIBLE)

        assertThat(visibilityItem.second).isEqualTo(emptyList<String>())
    }

    @Test
    fun updatingVisibilityTrackerWithManyItemsEmitsRightEvent() = runScenario {
        interactor.handleViewAction(
            viewAction = AddPaymentMethodInteractor.ViewAction.UpdatePaymentMethodVisibility(
                initialVisibilityTrackerData = MANY_ITEMS_ONE_PARTIALLY_VISIBLE,
            )
        )

        val visibilityItem = initialVisibilityTrackerTurbine.awaitItem()

        assertThat(visibilityItem.first).isEqualTo(MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_VISIBLE)

        assertThat(visibilityItem.second).isEqualTo(MANY_ITEMS_ONE_PARTIALLY_VISIBLE_EXPECTED_HIDDEN)
    }

    private fun runScenario(
        initiallySelectedPaymentMethodType: PaymentMethodCode = PaymentMethod.Type.Card.code,
        selection: StateFlow<PaymentSelection?> = MutableStateFlow(null),
        processing: StateFlow<Boolean> = MutableStateFlow(false),
        incentive: StateFlow<PaymentMethodIncentive?> = MutableStateFlow(null),
        supportedPaymentMethods: List<SupportedPaymentMethod> = emptyList(),
        createFormArguments: (PaymentMethodCode) -> FormArguments = {
            FormArguments(
                initiallySelectedPaymentMethodType,
                cbcEligibility = CardBrandChoiceEligibility.create(
                    isEligible = true,
                    preferredNetworks = emptyList(),
                ),
                merchantName = "Example, Inc.",
                hasIntentToSetup = false,
                paymentMethodSaveConsentBehavior = PaymentMethodSaveConsentBehavior.Legacy,
            )
        },
        formElementsForCode: (PaymentMethodCode) -> List<FormElement> = { emptyList() },
        createUSBankAccountFormArguments: (PaymentMethodCode) -> USBankAccountFormArguments = { mock() },
        dispatcher: TestDispatcher = StandardTestDispatcher(TestCoroutineScheduler()),
        testBlock: suspend TestParams.() -> Unit
    ) {
        val validationRequestedSource = MutableSharedFlow<Unit>()
        val reportFieldInteractionTurbine = Turbine<PaymentMethodCode>()
        val onFormFieldValuesChangedTurbine = Turbine<Pair<FormFieldValues?, String>>()
        val clearErrorMessagesTurbine = Turbine<Unit>()
        val reportPaymentMethodTypeSelectedTurbine = Turbine<PaymentMethodCode>()
        val initialVisibilityTrackerTurbine = Turbine<Pair<List<String>, List<String>>>()

        val interactor = DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initiallySelectedPaymentMethodType,
            selection = selection,
            processing = processing,
            incentive = incentive,
            supportedPaymentMethods = supportedPaymentMethods,
            createFormArguments = createFormArguments,
            formElementsForCode = formElementsForCode,
            clearErrorMessages = {
                clearErrorMessagesTurbine.add(Unit)
            },
            reportFieldInteraction = {
                reportFieldInteractionTurbine.add(it)
            },
            onFormFieldValuesChanged = { formFields: FormFieldValues?, paymentMethodCode: String ->
                onFormFieldValuesChangedTurbine.add(Pair(formFields, paymentMethodCode))
            },
            reportPaymentMethodTypeSelected = {
                reportPaymentMethodTypeSelectedTurbine.add(it)
            },
            createUSBankAccountFormArguments = createUSBankAccountFormArguments,
            coroutineScope = CoroutineScope(dispatcher),
            validationRequested = validationRequestedSource,
            isLiveMode = true,
            uiContext = dispatcher,
            onInitiallyDisplayedPaymentMethodVisibilitySnapshot = { visible, hidden ->
                initialVisibilityTrackerTurbine.add(Pair(visible, hidden))
            }
        )

        TestParams(
            interactor = interactor,
            dispatcher = dispatcher,
            validationRequestedSource = validationRequestedSource,
            reportFieldInteractionTurbine = reportFieldInteractionTurbine,
            onFormFieldValuesChangedTurbine = onFormFieldValuesChangedTurbine,
            clearErrorMessagesTurbine = clearErrorMessagesTurbine,
            reportPaymentMethodTypeSelectedTurbine = reportPaymentMethodTypeSelectedTurbine,
            initialVisibilityTrackerTurbine = initialVisibilityTrackerTurbine,
        ).apply {
            runTest {
                testBlock()
            }
            ensureAllEventsConsumed()
        }
    }

    private class TestParams(
        val interactor: AddPaymentMethodInteractor,
        val dispatcher: TestDispatcher,
        val validationRequestedSource: MutableSharedFlow<Unit>,
        val reportFieldInteractionTurbine: ReceiveTurbine<PaymentMethodCode>,
        val onFormFieldValuesChangedTurbine: ReceiveTurbine<Pair<FormFieldValues?, String>>,
        val clearErrorMessagesTurbine: ReceiveTurbine<Unit>,
        val reportPaymentMethodTypeSelectedTurbine: ReceiveTurbine<PaymentMethodCode>,
        val initialVisibilityTrackerTurbine: ReceiveTurbine<Pair<List<String>, List<String>>>
    ) {
        fun ensureAllEventsConsumed() {
            reportFieldInteractionTurbine.ensureAllEventsConsumed()
            onFormFieldValuesChangedTurbine.ensureAllEventsConsumed()
            clearErrorMessagesTurbine.ensureAllEventsConsumed()
            reportPaymentMethodTypeSelectedTurbine.ensureAllEventsConsumed()
            initialVisibilityTrackerTurbine.ensureAllEventsConsumed()
        }
    }
}
