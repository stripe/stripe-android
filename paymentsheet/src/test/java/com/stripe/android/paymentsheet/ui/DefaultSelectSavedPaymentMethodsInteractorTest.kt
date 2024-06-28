package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class DefaultSelectSavedPaymentMethodsInteractorTest {

    @Test
    fun initialState_isCorrect() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val expectedPaymentOptionsState = createPaymentOptionsState(paymentMethods)
        val expectedIsEditing = true
        val expectedIsProcessing = false

        runScenario(
            paymentOptionsState = MutableStateFlow(expectedPaymentOptionsState),
            editing = MutableStateFlow(expectedIsEditing),
            isProcessing = MutableStateFlow(expectedIsProcessing),
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsState).isEqualTo(expectedPaymentOptionsState)
                    assertThat(isEditing).isEqualTo(expectedIsEditing)
                    assertThat(isProcessing).isEqualTo(expectedIsProcessing)
                }
            }
        }
    }

    @Test
    fun updatingIsEditing_updatesState() {
        val initialIsEditingValue = false
        val isEditingFlow = MutableStateFlow(initialIsEditingValue)

        runScenario(editing = isEditingFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(isEditing).isEqualTo(initialIsEditingValue)
                }
            }

            isEditingFlow.value = !initialIsEditingValue

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(isEditing).isEqualTo(!initialIsEditingValue)
                }
            }
        }
    }

    @Test
    fun updatingIsProcessing_updatesState() {
        val initialIsProcessingValue = false
        val isProcessingFlow = MutableStateFlow(initialIsProcessingValue)

        runScenario(isProcessing = isProcessingFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(isProcessing).isEqualTo(initialIsProcessingValue)
                }
            }

            isProcessingFlow.value = !initialIsProcessingValue

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(isProcessing).isEqualTo(!initialIsProcessingValue)
                }
            }
        }
    }

    @Test
    fun updatingPaymentOptionsState_updatesState() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val initialPaymentOptionsState = createPaymentOptionsState(paymentMethods)
        val paymentOptionsStateFlow = MutableStateFlow(initialPaymentOptionsState)

        runScenario(paymentOptionsStateFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsState).isEqualTo(initialPaymentOptionsState)
                }
            }

            val newPaymentMethods = PaymentMethodFixtures.createCards(2)
            val newPaymentOptionsState = createPaymentOptionsState(newPaymentMethods)
            paymentOptionsStateFlow.value = newPaymentOptionsState

            dispatcher.scheduler.advanceUntilIdle()

            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsState).isEqualTo(newPaymentOptionsState)
                }
            }
        }
    }

    @Test
    fun handleViewAction_DeletePaymentMethod_deletesPaymentMethod() {
        var deletedPaymentMethodId: String? = null
        fun onDeletePaymentMethod(paymentMethodId: String?) {
            deletedPaymentMethodId = paymentMethodId
        }

        runScenario(onDeletePaymentMethod = ::onDeletePaymentMethod) {
            val paymentMethodToDelete = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.DeletePaymentMethod(
                    paymentMethodToDelete.id
                )
            )

            assertThat(deletedPaymentMethodId).isEqualTo(paymentMethodToDelete.id)
        }
    }

    @Test
    fun handleViewAction_EditPaymentMethod_editsPaymentMethod() {
        var editedPaymentMethodId: String? = null
        fun onEditPaymentMethod(paymentMethodId: String?) {
            editedPaymentMethodId = paymentMethodId
        }

        runScenario(onEditPaymentMethod = ::onEditPaymentMethod) {
            val paymentMethodToEdit = PaymentMethodFixtures.CARD_PAYMENT_METHOD
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod(
                    paymentMethodToEdit.id
                )
            )

            assertThat(editedPaymentMethodId).isEqualTo(paymentMethodToEdit.id)
        }
    }

    @Test
    fun handleViewAction_SelectPaymentMethod_selectsPaymentMethod() {
        var paymentSelection: PaymentSelection? = null
        fun onSelectPaymentMethod(selection: PaymentSelection?) {
            paymentSelection = selection
        }

        runScenario(onPaymentMethodSelected = ::onSelectPaymentMethod) {
            val newPaymentSelection = PaymentSelection.Saved(
                PaymentMethodFixtures.CARD_PAYMENT_METHOD
            )
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.SelectPaymentMethod(
                    newPaymentSelection
                )
            )

            assertThat(paymentSelection).isEqualTo(newPaymentSelection)
        }
    }

    @Test
    fun handleViewAction_AddCardPressed_callsOnAddCardPressed() {
        var addCardPressed = false
        fun onAddCardPressed() {
            addCardPressed = true
        }

        runScenario(
            onAddCardPressed = ::onAddCardPressed
        ) {
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.AddCardPressed
            )

            assertThat(addCardPressed).isTrue()
        }
    }

    private fun createPaymentOptionsState(paymentMethods: List<PaymentMethod>): PaymentOptionsState {
        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            nameProvider = { it!! },
            canRemovePaymentMethods = false,
            isCbcEligible = false,
        )
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        paymentOptionsState: StateFlow<PaymentOptionsState> = MutableStateFlow(
            PaymentOptionsState(
                items = emptyList(),
            )
        ),
        editing: StateFlow<Boolean> = MutableStateFlow(false),
        isProcessing: StateFlow<Boolean> = MutableStateFlow(false),
        onAddCardPressed: () -> Unit = { notImplemented() },
        onEditPaymentMethod: (String?) -> Unit = { notImplemented() },
        onDeletePaymentMethod: (String?) -> Unit = { notImplemented() },
        onPaymentMethodSelected: (PaymentSelection?) -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(TestCoroutineScheduler())

        val interactor = DefaultSelectSavedPaymentMethodsInteractor(
            paymentOptionsState,
            editing,
            isProcessing,
            onAddCardPressed,
            onEditPaymentMethod,
            onDeletePaymentMethod,
            onPaymentMethodSelected
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
        val interactor: SelectSavedPaymentMethodsInteractor,
        val dispatcher: TestDispatcher,
    )
}
