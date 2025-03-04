package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.analytics.EventReporter
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.ui.core.cbc.CardBrandChoiceEligibility
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class SavedPaymentMethodMutatorTest {
    @Test
    fun `canEdit is correct when no payment methods`() = runScenario {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                    paymentMethods = listOf()
                )
            )

            // Should still be false so expect no more events
            expectNoEvents()
        }
    }

    @Test
    fun `canEdit is correct when user has permissions to remove last PM`() = runScenario {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                )
            )
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `canEdit is correct when when user does not have permissions to remove last PM`() = runScenario {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = false,
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                    )
                )
            )
            assertThat(awaitItem()).isTrue()

            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = false,
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                    )
                )
            )
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `canEdit is correct CBC is enabled`() = runScenario(
        isCbcEligible = true
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = false,
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                    )
                )
            )
            assertThat(awaitItem()).isTrue()

            customerStateHolder.setCustomerState(null)
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = false,
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                    ),
                )
            )
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `removePaymentMethod triggers async removal`() {
        var calledDetach = false
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = { paymentMethodId ->
                assertThat(paymentMethodId).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD.id!!)
                calledDetach = true
                Result.failure(IllegalStateException())
            }
        )

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    ),
                )
            )

            customerStateHolder.paymentMethods.test {
                assertThat(awaitItem().size).isEqualTo(1)
                savedPaymentMethodMutator.removePaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                assertThat(awaitItem()).isEmpty()
            }

            assertThat(postPaymentMethodRemovedTurbine.awaitItem()).isEqualTo(Unit)

            assertThat(calledDetach).isTrue()
        }
    }

    @Test
    fun `removePaymentMethod with no CustomerConfiguration available, should not attempt detach`() = runScenario {
        var calledDetach = false
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = {
                calledDetach = true
                throw AssertionError("Not expected")
            }
        )

        runScenario(customerRepository = customerRepository) {
            savedPaymentMethodMutator.removePaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

            assertThat(calledDetach).isFalse()
        }
    }

    @Test
    fun `Sets editing to false when removing the last payment method while editing`() = runScenario {
        val customerPaymentMethods = PaymentMethodFixtures.createCards(1)
        customerStateHolder.setCustomerState(EMPTY_CUSTOMER_STATE.copy(paymentMethods = customerPaymentMethods))

        savedPaymentMethodMutator.editing.test {
            assertThat(awaitItem()).isFalse()

            savedPaymentMethodMutator.toggleEditing()
            assertThat(awaitItem()).isTrue()

            savedPaymentMethodMutator.removePaymentMethod(customerPaymentMethods.single())
            assertThat(awaitItem()).isFalse()
        }

        assertThat(postPaymentMethodRemovedTurbine.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `updatePaymentMethod should call through to the callback`() = runScenario {
        val cards = PaymentMethodFixtures.createCards(3)
        val expectedPaymentMethod = cards[0].toDisplayableSavedPaymentMethod()

        customerStateHolder.setCustomerState(
            createCustomerState(
                paymentMethods = cards,
                isRemoveEnabled = true,
                canRemoveLastPaymentMethod = true,
            )
        )

        savedPaymentMethodMutator.updatePaymentMethod(expectedPaymentMethod)
        updatePaymentMethodTurbine.awaitItem().apply {
            assertThat(paymentMethod).isEqualTo(expectedPaymentMethod)
            assertThat(canRemove).isTrue()
        }
    }

    @Test
    fun `updatePaymentMethod should be called correctly when 1 PM & cannot remove last PM`() = runScenario {
        val cards = PaymentMethodFixtures.createCards(1)
        val expectedPaymentMethod = cards[0].toDisplayableSavedPaymentMethod()

        customerStateHolder.setCustomerState(
            createCustomerState(
                paymentMethods = cards,
                isRemoveEnabled = true,
                canRemoveLastPaymentMethod = false,
            )
        )

        savedPaymentMethodMutator.updatePaymentMethod(expectedPaymentMethod)

        updatePaymentMethodTurbine.awaitItem().apply {
            assertThat(paymentMethod).isEqualTo(expectedPaymentMethod)
            assertThat(canRemove).isFalse()
        }
    }

    @Test
    fun `Removing selected payment method clears selection`() = runScenario {
        val cards = PaymentMethodFixtures.createCards(3)
        customerStateHolder.setCustomerState(EMPTY_CUSTOMER_STATE.copy(paymentMethods = cards))

        val selection = PaymentSelection.Saved(cards[1])
        selectionSource.value = selection

        selectionSource.test {
            assertThat(awaitItem()).isEqualTo(selection)
            savedPaymentMethodMutator.removePaymentMethod(selection.paymentMethod)
            assertThat(awaitItem()).isNull()
        }

        assertThat(postPaymentMethodRemovedTurbine.awaitItem()).isEqualTo(Unit)
    }

    @Test
    fun `On detach without remove duplicate permissions, should not attempt to remove duplicates in repository`() {
        removeDuplicatesTest(shouldRemoveDuplicates = false)
    }

    @Test
    fun `On detach with remove duplicate permissions, should attempt to remove duplicates in repository`() {
        removeDuplicatesTest(shouldRemoveDuplicates = true)
    }

    @Test
    fun `updatePaymentMethod calls through to callback`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        runScenario {
            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            updatePaymentMethodTurbine.awaitItem().apply {
                assertThat(paymentMethod).isEqualTo(displayableSavedPaymentMethod)
                assertThat(canRemove).isFalse()
            }
        }
    }

    @Test
    fun `updatePaymentMethod performRemove callback`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val calledDetach = Turbine<Boolean>()
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = { paymentMethodId ->
                assertThat(paymentMethodId).isEqualTo(displayableSavedPaymentMethod.paymentMethod.id!!)
                calledDetach.add(true)
                Result.success(displayableSavedPaymentMethod.paymentMethod)
            }
        )

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = listOf(displayableSavedPaymentMethod.paymentMethod),
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                )
            )

            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            updatePaymentMethodTurbine.awaitItem().performRemove()

            assertThat(calledDetach.awaitItem()).isTrue()
            assertThat(prePaymentMethodRemovedTurbine.awaitItem()).isNotNull()
            assertThat(postPaymentMethodRemovedTurbine.awaitItem()).isNotNull()

            assertThat(customerStateHolder.paymentMethods.value).isEmpty()
        }

        calledDetach.ensureAllEventsConsumed()
    }

    @Test
    fun `removePaymentMethodInEditScreen calls prePaymentMethodRemoveActions and postPaymentMethodRemoveActions`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val calledDetach = Turbine<Boolean>()
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = { paymentMethodId ->
                assertThat(paymentMethodId).isEqualTo(displayableSavedPaymentMethod.paymentMethod.id!!)
                calledDetach.add(true)
                Result.success(displayableSavedPaymentMethod.paymentMethod)
            }
        )

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = listOf(displayableSavedPaymentMethod.paymentMethod),
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                )
            )

            savedPaymentMethodMutator.removePaymentMethodInEditScreen(displayableSavedPaymentMethod.paymentMethod)

            assertThat(calledDetach.awaitItem()).isTrue()
            assertThat(prePaymentMethodRemovedTurbine.awaitItem()).isNotNull()
            assertThat(postPaymentMethodRemovedTurbine.awaitItem()).isNotNull()

            assertThat(customerStateHolder.paymentMethods.value).isEmpty()
        }

        calledDetach.ensureAllEventsConsumed()
    }

    @Test
    fun `updatePaymentMethod performRemove failure callback`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val calledDetach = Turbine<Boolean>()
        val customerRepository = FakeCustomerRepository(
            onDetachPaymentMethod = { paymentMethodId ->
                assertThat(paymentMethodId).isEqualTo(displayableSavedPaymentMethod.paymentMethod.id!!)
                calledDetach.add(true)
                Result.failure(IllegalStateException("Test failure."))
            }
        )

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = listOf(displayableSavedPaymentMethod.paymentMethod),
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                )
            )

            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            updatePaymentMethodTurbine.awaitItem().performRemove()

            assertThat(calledDetach.awaitItem()).isTrue()

            assertThat(customerStateHolder.paymentMethods.value).hasSize(1)
        }

        calledDetach.ensureAllEventsConsumed()
    }

    @Test
    fun `updatePaymentMethod updateExecutor callback`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val calledUpdate = Turbine<Boolean>()
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
                calledUpdate.add(true)
                Result.success(
                    displayableSavedPaymentMethod.paymentMethod.copy(
                        card = displayableSavedPaymentMethod.paymentMethod.card?.copy(brand = CardBrand.CartesBancaires)
                    )
                )
            }
        )

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = listOf(displayableSavedPaymentMethod.paymentMethod),
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                )
            )

            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            assertThat(customerStateHolder.paymentMethods.value.first().card?.brand).isEqualTo(CardBrand.Unknown)
            updatePaymentMethodTurbine.awaitItem().updateExecutor(CardBrand.CartesBancaires)

            assertThat(calledUpdate.awaitItem()).isTrue()

            val paymentMethods = customerStateHolder.paymentMethods.value
            assertThat(paymentMethods).hasSize(1)
            assertThat(paymentMethods.first().card?.brand).isEqualTo(CardBrand.CartesBancaires)
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `modifyCardPaymentMethod updates card`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val calledUpdate = Turbine<Boolean>()
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
                calledUpdate.add(true)
                Result.success(
                    displayableSavedPaymentMethod.paymentMethod.copy(
                        card = displayableSavedPaymentMethod.paymentMethod.card?.copy(brand = CardBrand.CartesBancaires)
                    )
                )
            }
        )

        val eventReporter = mock<EventReporter>()

        runScenario(eventReporter = eventReporter, customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = listOf(displayableSavedPaymentMethod.paymentMethod),
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                )
            )

            assertThat(customerStateHolder.paymentMethods.value.first().card?.brand).isEqualTo(CardBrand.Unknown)

            savedPaymentMethodMutator.modifyCardPaymentMethod(
                paymentMethod = displayableSavedPaymentMethod.paymentMethod,
                brand = CardBrand.CartesBancaires,
            )

            assertThat(calledUpdate.awaitItem()).isTrue()

            val paymentMethods = customerStateHolder.paymentMethods.value
            assertThat(paymentMethods).hasSize(1)
            assertThat(paymentMethods.first().card?.brand).isEqualTo(CardBrand.CartesBancaires)

            verify(eventReporter).onUpdatePaymentMethodSucceeded(
                selectedBrand = CardBrand.CartesBancaires,
            )
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `updatePaymentMethod updateExecutor failure callback`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val calledUpdate = Turbine<Boolean>()
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
                calledUpdate.add(true)
                Result.failure(IllegalStateException("Test failure"))
            }
        )

        val eventReporter = mock<EventReporter>()

        runScenario(eventReporter = eventReporter, customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = listOf(displayableSavedPaymentMethod.paymentMethod),
                    isRemoveEnabled = true,
                    canRemoveLastPaymentMethod = true,
                )
            )

            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            assertThat(customerStateHolder.paymentMethods.value.first().card?.brand).isEqualTo(CardBrand.Unknown)
            updatePaymentMethodTurbine.awaitItem().updateExecutor(CardBrand.CartesBancaires)

            assertThat(calledUpdate.awaitItem()).isTrue()

            val paymentMethods = customerStateHolder.paymentMethods.value
            assertThat(paymentMethods).hasSize(1)
            assertThat(paymentMethods.first().card?.brand).isEqualTo(CardBrand.Unknown)

            verify(eventReporter).onUpdatePaymentMethodFailed(
                selectedBrand = eq(CardBrand.CartesBancaires),
                error = argThat { message == "Test failure" }
            )
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `setDefaultPaymentMethod updates default payment method on success`() {
        val customerRepository = FakeCustomerRepository(
            onSetDefaultPaymentMethod = { Result.success(mock()) }
        )
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = paymentMethods.first().id,
                )
            )

            val newDefaultPaymentMethod = paymentMethods[1]
            savedPaymentMethodMutator.setDefaultPaymentMethod(newDefaultPaymentMethod)

            customerStateHolder.customer.test {
                assertThat(awaitItem()?.defaultPaymentMethodId).isEqualTo(newDefaultPaymentMethod.id)
            }
        }
    }

    @Test
    fun `setDefaultPaymentMethod updates selection on success`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        runScenario(
            customerRepository = FakeCustomerRepository(
                onSetDefaultPaymentMethod = { Result.success(mock()) }
            )
        ) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = paymentMethods.first().id,
                )
            )

            val newDefaultPaymentMethod = paymentMethods[1]
            savedPaymentMethodMutator.setDefaultPaymentMethod(newDefaultPaymentMethod)

            selectionSource.test {
                assertThat(awaitItem()).isEqualTo(PaymentSelection.Saved(newDefaultPaymentMethod))
            }
        }
    }

    @Test
    fun `setDefaultPaymentMethod success updateExecutor callback`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val calledUpdate = Turbine<Boolean>()

        val customerRepository = FakeCustomerRepository(
            onSetDefaultPaymentMethod = {
                calledUpdate.add(true)
                Result.success(mock())
            }
        )

        val eventReporter = mock<EventReporter>()

        runScenario(eventReporter = eventReporter, customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = paymentMethods.first().id,
                )
            )

            val newDefaultPaymentMethod = paymentMethods[1]
            savedPaymentMethodMutator.setDefaultPaymentMethod(newDefaultPaymentMethod)

            assertThat(calledUpdate.awaitItem()).isTrue()

            val defaultPaymentMethodId = customerStateHolder.customer.value?.defaultPaymentMethodId

            assertThat(defaultPaymentMethodId).isEqualTo(newDefaultPaymentMethod.id)
            verify(eventReporter).onSetAsDefaultPaymentMethodSucceeded()
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `setDefaultPaymentMethod failed updateExecutor callback`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val calledUpdate = Turbine<Boolean>()

        val customerRepository = FakeCustomerRepository(
            onSetDefaultPaymentMethod = {
                calledUpdate.add(true)
                Result.failure(IllegalStateException("Test failure"))
            }
        )

        val eventReporter = mock<EventReporter>()

        runScenario(eventReporter = eventReporter, customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = paymentMethods.first().id,
                )
            )

            val newDefaultPaymentMethod = paymentMethods[1]
            savedPaymentMethodMutator.setDefaultPaymentMethod(newDefaultPaymentMethod)

            assertThat(calledUpdate.awaitItem()).isTrue()

            val defaultPaymentMethodId = customerStateHolder.customer.value?.defaultPaymentMethodId

            assertThat(defaultPaymentMethodId).isEqualTo(paymentMethods.first().id)
            verify(eventReporter).onSetAsDefaultPaymentMethodFailed(
                error = argThat {
                    message == "Test failure"
                }
            )
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    private fun removeDuplicatesTest(shouldRemoveDuplicates: Boolean) {
        val repository = FakeCustomerRepository()

        runScenario(repository) {
            customerStateHolder.setCustomerState(
                CustomerState(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_1",
                    customerSessionClientSecret = null,
                    paymentMethods = listOf(),
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = true,
                        canRemoveLastPaymentMethod = true,
                        canRemoveDuplicates = shouldRemoveDuplicates,
                    ),
                    defaultPaymentMethodId = null,
                )
            )

            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

            savedPaymentMethodMutator.removePaymentMethod(paymentMethod)

            assertThat(postPaymentMethodRemovedTurbine.awaitItem()).isEqualTo(Unit)

            assertThat(repository.detachRequests.awaitItem()).isEqualTo(
                FakeCustomerRepository.DetachRequest(
                    paymentMethodId = paymentMethod.id!!,
                    customerInfo = CustomerRepository.CustomerInfo(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_1",
                        customerSessionClientSecret = null,
                    ),
                    canRemoveDuplicates = shouldRemoveDuplicates,
                )
            )
        }
    }

    @Suppress("LongMethod")
    private fun runScenario(
        customerRepository: CustomerRepository = FakeCustomerRepository(),
        eventReporter: EventReporter? = null,
        isCbcEligible: Boolean = false,
        block: suspend Scenario.() -> Unit
    ) {
        runTest {
            val selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
            val currentScreen: MutableStateFlow<PaymentSheetScreen> = MutableStateFlow(PaymentSheetScreen.Loading)

            val customerStateHolder = CustomerStateHolder(
                savedStateHandle = SavedStateHandle(),
                selection = selection,
            )

            val postPaymentMethodRemovedTurbine = Turbine<Unit>()
            val prePaymentMethodRemovedTurbine = Turbine<Unit>()
            val updatePaymentMethodTurbine = Turbine<UpdateCall>()

            val savedPaymentMethodMutator = SavedPaymentMethodMutator(
                paymentMethodMetadataFlow = stateFlowOf(
                    PaymentMethodMetadataFactory.create(
                        cbcEligibility = if (isCbcEligible) {
                            CardBrandChoiceEligibility.Eligible(listOf(CardBrand.Visa, CardBrand.CartesBancaires))
                        } else {
                            CardBrandChoiceEligibility.Ineligible
                        }
                    )
                ),
                eventReporter = eventReporter ?: mock(),
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                workContext = coroutineContext,
                uiContext = coroutineContext,
                customerRepository = customerRepository,
                selection = selection,
                setSelection = { selection.value = it },
                customerStateHolder = customerStateHolder,
                prePaymentMethodRemoveActions = { prePaymentMethodRemovedTurbine.add(Unit) },
                postPaymentMethodRemoveActions = { postPaymentMethodRemovedTurbine.add(Unit) },
                onUpdatePaymentMethod = {
                        displayableSavedPaymentMethod,
                        canRemove,
                        performRemove,
                        updateExecutor,
                        setDefaultPaymentMethodExecutor, ->
                    updatePaymentMethodTurbine.add(
                        UpdateCall(
                            displayableSavedPaymentMethod,
                            canRemove,
                            performRemove,
                            updateExecutor,
                            setDefaultPaymentMethodExecutor
                        )
                    )
                },
                isLinkEnabled = stateFlowOf(false),
                isNotPaymentFlow = true,
            )
            Scenario(
                savedPaymentMethodMutator = savedPaymentMethodMutator,
                customerStateHolder = customerStateHolder,
                selectionSource = selection,
                currentScreen = currentScreen,
                prePaymentMethodRemovedTurbine = prePaymentMethodRemovedTurbine,
                postPaymentMethodRemovedTurbine = postPaymentMethodRemovedTurbine,
                updatePaymentMethodTurbine = updatePaymentMethodTurbine,
                testScope = this,
            ).apply {
                block()
            }

            advanceUntilIdle()

            postPaymentMethodRemovedTurbine.ensureAllEventsConsumed()
            updatePaymentMethodTurbine.ensureAllEventsConsumed()
        }
    }

    private data class Scenario(
        val savedPaymentMethodMutator: SavedPaymentMethodMutator,
        val customerStateHolder: CustomerStateHolder,
        val selectionSource: MutableStateFlow<PaymentSelection?>,
        val currentScreen: MutableStateFlow<PaymentSheetScreen>,
        val prePaymentMethodRemovedTurbine: ReceiveTurbine<Unit>,
        val postPaymentMethodRemovedTurbine: ReceiveTurbine<Unit>,
        val updatePaymentMethodTurbine: ReceiveTurbine<UpdateCall>,
        val testScope: TestScope,
    )

    private data class UpdateCall(
        val paymentMethod: DisplayableSavedPaymentMethod,
        val canRemove: Boolean,
        val performRemove: suspend () -> Throwable?,
        val updateExecutor: suspend (brand: CardBrand) -> Result<PaymentMethod>,
        val setSetDefaultPaymentMethodExecutor: suspend (paymentMethod: PaymentMethod) -> Result<Unit>,
    )
}
