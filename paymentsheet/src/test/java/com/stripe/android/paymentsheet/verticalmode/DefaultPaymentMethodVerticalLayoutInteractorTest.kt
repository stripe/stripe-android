package com.stripe.android.paymentsheet.verticalmode

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.PaymentMethodCreateParams
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.forms.FormFieldValues
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor.ViewAction
import com.stripe.android.ui.core.elements.SaveForFutureUseElement
import com.stripe.android.uicore.elements.FormElement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock

class DefaultPaymentMethodVerticalLayoutInteractorTest {
    @Test
    fun state_updatesWhenProcessingUpdates() = runScenario {
        interactor.state.test {
            awaitItem().run {
                assertThat(isProcessing).isFalse()
            }
            processingSource.value = true
            awaitItem().run {
                assertThat(isProcessing).isTrue()
            }
        }
    }

    @Test
    fun state_updatesWhenSelectionUpdates() = runScenario(
        paymentMethodMetadata = PaymentMethodMetadataFactory.create(
            stripeIntent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
                paymentMethodTypes = listOf("card", "cashapp")
            )
        )
    ) {
        interactor.state.test {
            awaitItem().run {
                assertThat(supportedPaymentMethods).isNotEmpty()
                assertThat(selectedPaymentMethodIndex).isEqualTo(-1)
            }
            selectionSource.value = PaymentSelection.New.GenericPaymentMethod(
                labelResource = "CashApp",
                iconResource = 0,
                lightThemeIconUrl = null,
                darkThemeIconUrl = null,
                paymentMethodCreateParams = PaymentMethodCreateParams.createCashAppPay(),
                customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest,
            )
            awaitItem().run {
                assertThat(supportedPaymentMethods).isNotEmpty()
                assertThat(selectedPaymentMethodIndex).isEqualTo(1)
            }
        }
    }

    @Test
    fun state_returnsCorrectSelectedPaymentMethodIndexWhenSelectionIsSaved() = runScenario {
        interactor.state.test {
            awaitItem().run {
                assertThat(supportedPaymentMethods).isNotEmpty()
                assertThat(selectedPaymentMethodIndex).isEqualTo(-1)
            }
            selectionSource.value = PaymentSelection.Saved(
                paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD,
            )
            // No value produced, since the state is the same.
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenFieldsAllowUserInteraction() {
        var calledFormScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            formElementsForCode = {
                listOf(SaveForFutureUseElement(true, "Jay's Ski Shop"))
            },
            formScreenFactory = {
                calledFormScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("card"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenSelectedIsUsBank() {
        var calledFormScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            formElementsForCode = {
                listOf()
            },
            formScreenFactory = {
                calledFormScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("us_bank_account"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_transitionsToFormScreen_whenSelectedIsInstantDebits() {
        var calledFormScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            formElementsForCode = {
                listOf()
            },
            formScreenFactory = {
                calledFormScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("link"))
            assertThat(calledFormScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    @Test
    fun handleViewAction_PaymentMethodSelected_updatesSelectedLPM() {
        var onFormFieldValuesChangedCalled = false
        runScenario(
            formElementsForCode = {
                listOf()
            },
            onFormFieldValuesChanged = { fieldValues, selectedPaymentMethodCode ->
                fieldValues.run {
                    assertThat(fieldValuePairs).isEmpty()
                    assertThat(userRequestedReuse).isEqualTo(PaymentSelection.CustomerRequestedSave.NoRequest)
                }
                assertThat(selectedPaymentMethodCode).isEqualTo("cashapp")
                onFormFieldValuesChangedCalled = true
            }
        ) {
            interactor.handleViewAction(ViewAction.PaymentMethodSelected("cashapp"))
            assertThat(onFormFieldValuesChangedCalled).isTrue()
        }
    }

    @Test
    fun handleViewAction_TransitionToManageSavedPaymentMethods_transitionsToManageScreen() {
        var calledManageScreenFactory = false
        var calledTransitionTo = false
        runScenario(
            manageScreenFactory = {
                calledManageScreenFactory = true
                mock()
            },
            transitionTo = {
                calledTransitionTo = true
            }
        ) {
            interactor.handleViewAction(ViewAction.TransitionToManageSavedPaymentMethods)
            assertThat(calledManageScreenFactory).isTrue()
            assertThat(calledTransitionTo).isTrue()
        }
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        paymentMethodMetadata: PaymentMethodMetadata = PaymentMethodMetadataFactory.create(),
        initialProcessing: Boolean = false,
        initialSelection: PaymentSelection? = null,
        formElementsForCode: (code: String) -> List<FormElement> = { notImplemented() },
        transitionTo: (screen: PaymentSheetScreen) -> Unit = { notImplemented() },
        onFormFieldValuesChanged: (formValues: FormFieldValues, selectedPaymentMethodCode: String) -> Unit = { _, _ ->
            notImplemented()
        },
        manageScreenFactory: () -> PaymentSheetScreen = { notImplemented() },
        formScreenFactory: (selectedPaymentMethodCode: String) -> PaymentSheetScreen = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit
    ) {
        val processing: MutableStateFlow<Boolean> = MutableStateFlow(initialProcessing)
        val selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(initialSelection)

        val interactor = DefaultPaymentMethodVerticalLayoutInteractor(
            paymentMethodMetadata = paymentMethodMetadata,
            processing = processing,
            selection = selection,
            formElementsForCode = formElementsForCode,
            transitionTo = transitionTo,
            onFormFieldValuesChanged = onFormFieldValuesChanged,
            manageScreenFactory = manageScreenFactory,
            formScreenFactory = formScreenFactory,
        )

        TestParams(
            processingSource = processing,
            selectionSource = selection,
            interactor = interactor,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private class TestParams(
        val processingSource: MutableStateFlow<Boolean>,
        val selectionSource: MutableStateFlow<PaymentSelection?>,
        val interactor: PaymentMethodVerticalLayoutInteractor,
    )
}
