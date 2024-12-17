package com.stripe.android.paymentsheet.ui

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsStateFactory
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.utils.BankFormScreenStateFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

class DefaultSelectSavedPaymentMethodsInteractorTest {

    @Test
    fun initialState_isCorrect() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)
        val expectedPaymentOptionsItems = createPaymentOptionsItems(paymentMethods)
        val expectedSelectedPaymentMethod = paymentMethods[1]
        val expectedIsEditing = true
        val expectedIsProcessing = false

        runScenario(
            paymentOptionsItems = MutableStateFlow(expectedPaymentOptionsItems),
            currentSelection = MutableStateFlow(PaymentSelection.Saved(expectedSelectedPaymentMethod)),
            editing = MutableStateFlow(expectedIsEditing),
            isProcessing = MutableStateFlow(expectedIsProcessing),
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsItems).isEqualTo(expectedPaymentOptionsItems)
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(expectedSelectedPaymentMethod)
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

            interactor.state.test {
                awaitItem().run {
                    assertThat(isEditing).isEqualTo(!initialIsEditingValue)
                }
            }
        }
    }

    @Test
    fun updatingCanEdit_updatesState() {
        val canEdit = MutableStateFlow(true)
        runScenario(canEdit = canEdit) {
            interactor.state.test {
                assertThat(awaitItem().canEdit).isTrue()

                canEdit.value = false
                assertThat(awaitItem().canEdit).isFalse()
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
        val initialPaymentOptionsItems = createPaymentOptionsItems(paymentMethods)
        val paymentOptionsStateFlow = MutableStateFlow(initialPaymentOptionsItems)

        runScenario(paymentOptionsStateFlow) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsItems).isEqualTo(initialPaymentOptionsItems)
                }
            }

            val newPaymentMethods = PaymentMethodFixtures.createCards(2)
            val newPaymentOptionsState = createPaymentOptionsItems(newPaymentMethods)
            paymentOptionsStateFlow.value = newPaymentOptionsState

            interactor.state.test {
                awaitItem().run {
                    assertThat(paymentOptionsItems).isEqualTo(newPaymentOptionsState)
                }
            }
        }
    }

    @Test
    fun handleViewAction_EditPaymentMethod_updatesPaymentMethod() {
        var updatedPaymentMethod: DisplayableSavedPaymentMethod? = null
        fun onUpdatePaymentMethod(paymentMethod: DisplayableSavedPaymentMethod) {
            updatedPaymentMethod = paymentMethod
        }

        runScenario(onUpdatePaymentMethod = ::onUpdatePaymentMethod) {
            val paymentMethodToUpdate = PaymentMethodFixtures.displayableCard()
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.EditPaymentMethod(
                    paymentMethodToUpdate
                )
            )

            assertThat(updatedPaymentMethod).isEqualTo(paymentMethodToUpdate)
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

    @Test
    fun handleViewAction_ToggleEdit_calls_toggleEdit() {
        var hasCalledToggleEdit = false
        runScenario(
            toggleEdit = { hasCalledToggleEdit = true }
        ) {
            interactor.handleViewAction(
                SelectSavedPaymentMethodsInteractor.ViewAction.ToggleEdit
            )

            assertThat(hasCalledToggleEdit).isTrue()
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsLink() {
        val currentSelectionFlow = MutableStateFlow(PaymentSelection.Link)

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(
                    paymentMethods = PaymentMethodFixtures.createCards(2),
                ).plus(PaymentOptionsItem.Link)
            ),
            currentSelection = currentSelectionFlow,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsLink_canBeChangedToGooglePay() {
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> =
            MutableStateFlow(PaymentSelection.Link)

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(
                    paymentMethods = PaymentMethodFixtures.createCards(2),
                ).plus(PaymentOptionsItem.Link).plus(PaymentOptionsItem.GooglePay)
            ),
            currentSelection = currentSelectionFlow,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }

            currentSelectionFlow.value = PaymentSelection.GooglePay

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.GooglePay)
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsLink_doesNotChangeWhenSelectionBecomesNew() {
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> =
            MutableStateFlow(PaymentSelection.Link)

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(
                    paymentMethods = PaymentMethodFixtures.createCards(2),
                ).plus(PaymentOptionsItem.Link)
            ),
            currentSelection = currentSelectionFlow,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }

            currentSelectionFlow.value = newPaymentSelection()

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(PaymentOptionsItem.Link)
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsNull_usesMostRecentlySavedSelection() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(createPaymentOptionsItems(paymentMethods = paymentMethods)),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        selectedSavedPaymentMethod
                    )
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_currentSelectionIsNull_respondsToChangesToMostRecentlySavedSelection() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(createPaymentOptionsItems(paymentMethods = paymentMethods)),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        selectedSavedPaymentMethod
                    )
                }
            }

            mostRecentlySelectedSavedPaymentMethod.value = paymentMethods[0]

            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        paymentMethods[0]
                    )
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_canChangeFromSaved_toLink() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(
                createPaymentOptionsItems(paymentMethods = paymentMethods).plus(
                    PaymentOptionsItem.Link
                )
            ),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(
                        selectedSavedPaymentMethod
                    )
                }
            }

            currentSelectionFlow.value = PaymentSelection.Link

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isEqualTo(
                        PaymentOptionsItem.Link
                    )
                }
            }
        }
    }

    @Test
    fun selectedPaymentOptionItem_savedPaymentSelectionRemoved_newSelectionIsNull() {
        val paymentMethods = PaymentMethodFixtures.createCards(2)
        val selectedSavedPaymentMethod: PaymentMethod = paymentMethods[1]
        val currentSelectionFlow: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
        val mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(
            selectedSavedPaymentMethod
        )

        runScenario(
            paymentOptionsItems = MutableStateFlow(createPaymentOptionsItems(paymentMethods = paymentMethods)),
            currentSelection = currentSelectionFlow,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
        ) {
            interactor.state.test {
                awaitItem().run {
                    assertThat(
                        (selectedPaymentOptionsItem as? PaymentOptionsItem.SavedPaymentMethod)?.paymentMethod
                    ).isEqualTo(selectedSavedPaymentMethod)
                }
            }

            currentSelectionFlow.value = null
            mostRecentlySelectedSavedPaymentMethod.value = null

            interactor.state.test {
                awaitItem().run {
                    assertThat(selectedPaymentOptionsItem).isNull()
                }
            }
        }
    }

    private fun createPaymentOptionsItems(
        paymentMethods: List<PaymentMethod>,
    ): List<PaymentOptionsItem> {
        return PaymentOptionsStateFactory.create(
            paymentMethods = paymentMethods,
            showGooglePay = false,
            showLink = false,
            currentSelection = PaymentSelection.Saved(paymentMethods[0]),
            nameProvider = { it!!.resolvableString },
            isCbcEligible = true,
            defaultPaymentMethodId = null,
        ).items
    }

    private fun newPaymentSelection(): PaymentSelection.New {
        return PaymentSelection.New.USBankAccount(
            label = "Test",
            iconResource = 0,
            paymentMethodCreateParams = mock(),
            customerRequestedSave = mock(),
            input = PaymentSelection.New.USBankAccount.Input(
                name = "",
                email = null,
                phone = null,
                address = null,
                saveForFutureUse = false,
            ),
            instantDebits = null,
            screenState = BankFormScreenStateFactory.createWithSession("session_1234"),
        )
    }

    private val notImplemented: () -> Nothing = { throw AssertionError("Not implemented") }

    private fun runScenario(
        paymentOptionsItems: StateFlow<List<PaymentOptionsItem>> = MutableStateFlow(emptyList()),
        editing: StateFlow<Boolean> = MutableStateFlow(false),
        canEdit: StateFlow<Boolean> = MutableStateFlow(true),
        canRemove: StateFlow<Boolean> = MutableStateFlow(true),
        toggleEdit: () -> Unit = { notImplemented() },
        isProcessing: StateFlow<Boolean> = MutableStateFlow(false),
        currentSelection: StateFlow<PaymentSelection?> = MutableStateFlow(null),
        mostRecentlySelectedSavedPaymentMethod: MutableStateFlow<PaymentMethod?> = MutableStateFlow(null),
        onAddCardPressed: () -> Unit = { notImplemented() },
        onUpdatePaymentMethod: (DisplayableSavedPaymentMethod) -> Unit = { notImplemented() },
        onPaymentMethodSelected: (PaymentSelection?) -> Unit = { notImplemented() },
        testBlock: suspend TestParams.() -> Unit,
    ) {
        val interactor = DefaultSelectSavedPaymentMethodsInteractor(
            paymentOptionsItems = paymentOptionsItems,
            editing = editing,
            canEdit = canEdit,
            canRemove = canRemove,
            toggleEdit = toggleEdit,
            isProcessing = isProcessing,
            currentSelection = currentSelection,
            mostRecentlySelectedSavedPaymentMethod = mostRecentlySelectedSavedPaymentMethod,
            onAddCardPressed = onAddCardPressed,
            onUpdatePaymentMethod = onUpdatePaymentMethod,
            onPaymentMethodSelected = onPaymentMethodSelected,
            isLiveMode = true,
        )

        TestParams(
            interactor = interactor,
        ).apply {
            runTest {
                testBlock()
            }
        }
    }

    private class TestParams(
        val interactor: SelectSavedPaymentMethodsInteractor,
    )
}
