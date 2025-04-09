package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.luxe.SupportedPaymentMethod
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodSaveConsentBehavior
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodCode
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentMethodIncentive
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.paymentdatacollection.FormArguments
import com.stripe.android.paymentsheet.paymentdatacollection.ach.USBankAccountFormArguments
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.mock
import kotlin.test.Test

class DefaultAddPaymentMethodInteractorTest {
    @Test
    fun handleViewAction_ReportFieldInteraction_reportsFieldInteraction() {
        var latestCodeWithInteraction: PaymentMethodCode? = null
        fun reportFieldInteraction(code: PaymentMethodCode) {
            latestCodeWithInteraction = code
        }

        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            reportFieldInteraction = ::reportFieldInteraction,
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.ReportFieldInteraction(expectedCode)
            )

            assertThat(latestCodeWithInteraction).isEqualTo(expectedCode)
        }
    }

    @Test
    fun handleViewAction_OnFormFieldValuesChanged_updatesFormFields() {
        var latestCodeWithChangedFormFields: PaymentMethodCode? = null
        fun onFormFieldValuesChanged(code: PaymentMethodCode) {
            latestCodeWithChangedFormFields = code
        }

        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            onFormFieldValuesChanged = { _, code -> onFormFieldValuesChanged(code) },
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnFormFieldValuesChanged(null, expectedCode)
            )

            assertThat(latestCodeWithChangedFormFields).isEqualTo(expectedCode)
        }
    }

    @Test
    fun handleViewAction_OnPaymentMethodSelected_selectsPaymentMethod() {
        var reportedSelectedPaymentMethodCode: PaymentMethodCode? = null
        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
            reportPaymentMethodTypeSelected = { code, _ -> reportedSelectedPaymentMethodCode = code },
            clearErrorMessages = {},
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(expectedCode)
            )

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(reportedSelectedPaymentMethodCode).isEqualTo(expectedCode)
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentMethodCode).isEqualTo(expectedCode)
                }
            }
        }
    }

    @Test
    fun handleViewAction_OnPaymentMethodSelected_withoutNewPaymentMethod_doesntReportSelection() {
        var reportedSelectedPaymentMethodCode: PaymentMethodCode? = null
        val expectedCode = PaymentMethod.Type.CashAppPay.code

        runScenario(
            initiallySelectedPaymentMethodType = expectedCode,
            reportPaymentMethodTypeSelected = { code, _ -> reportedSelectedPaymentMethodCode = code },
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(expectedCode)
            )

            assertThat(reportedSelectedPaymentMethodCode).isNull()
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
            reportPaymentMethodTypeSelected = { _, _ -> },
            clearErrorMessages = {},
        ) {
            val newPaymentMethodCode = PaymentMethod.Type.CashAppPay.code
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(
                    newPaymentMethodCode
                )
            )

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
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
        var errorMessagesHaveBeenCleared = false
        fun clearErrorMessages() {
            errorMessagesHaveBeenCleared = true
        }

        runScenario(
            initiallySelectedPaymentMethodType = PaymentMethod.Type.Card.code,
            clearErrorMessages = ::clearErrorMessages,
            reportPaymentMethodTypeSelected = { _, _ -> },
        ) {
            interactor.handleViewAction(
                AddPaymentMethodInteractor.ViewAction.OnPaymentMethodSelected(
                    PaymentMethod.Type.CashAppPay.code,
                )
            )

            dispatcher.scheduler.advanceUntilIdle()

            assertThat(errorMessagesHaveBeenCleared).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

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
        clearErrorMessages: () -> Unit = { notImplemented() },
        reportFieldInteraction: (PaymentMethodCode) -> Unit = { notImplemented() },
        onFormFieldValuesChanged: (FormFieldValues?, String) -> Unit = { _, _ -> notImplemented() },
        reportPaymentMethodTypeSelected: (PaymentMethodCode, Boolean) -> Unit = { _, _ -> notImplemented() },
        createUSBankAccountFormArguments: (PaymentMethodCode) -> USBankAccountFormArguments = { mock() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

        val interactor = DefaultAddPaymentMethodInteractor(
            initiallySelectedPaymentMethodType = initiallySelectedPaymentMethodType,
            selection = selection,
            processing = processing,
            incentive = incentive,
            supportedPaymentMethods = supportedPaymentMethods,
            createFormArguments = createFormArguments,
            formElementsForCode = formElementsForCode,
            clearErrorMessages = clearErrorMessages,
            reportFieldInteraction = reportFieldInteraction,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            reportPaymentMethodTypeSelected = reportPaymentMethodTypeSelected,
            createUSBankAccountFormArguments = createUSBankAccountFormArguments,
            coroutineScope = CoroutineScope(dispatcher),
            isLiveMode = true,
        )

        TestParams(
            interactor = interactor,
            dispatcher = dispatcher,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private class TestParams(
        val interactor: AddPaymentMethodInteractor,
        val dispatcher: TestDispatcher,
    )
}
