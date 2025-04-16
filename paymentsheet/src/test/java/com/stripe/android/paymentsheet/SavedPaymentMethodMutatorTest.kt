package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.Turbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadata
import com.stripe.android.lpmfoundations.paymentmethod.PaymentMethodMetadataFactory
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.analytics.FakeEventReporter
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.mock

@Suppress("LargeClass")
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
        paymentMethodMetadataFlow = stateFlowOf(
            PaymentMethodMetadataFactory.create(
                cbcEligibility = CardBrandChoiceEligibility.Eligible(listOf(CardBrand.Visa, CardBrand.CartesBancaires))
            )
        )
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
        assertThat(eventReporter.removePaymentMethodCalls.awaitItem().code).isEqualTo("card")
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
            updatePaymentMethodTurbine.awaitItem()
                .updateExecutor(CardUpdateParams(cardBrand = CardBrand.CartesBancaires))

            assertThat(calledUpdate.awaitItem()).isTrue()

            val paymentMethods = customerStateHolder.paymentMethods.value
            assertThat(paymentMethods).hasSize(1)
            assertThat(paymentMethods.first().card?.brand).isEqualTo(CardBrand.CartesBancaires)
            eventReporter.assertUpdatePaymentMethodSucceededCalls(CardBrand.CartesBancaires)
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `updatePaymentMethod updateExecutor analytics events received`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
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

            updatePaymentMethodTurbine.awaitItem()
                .updateExecutor(CardUpdateParams(cardBrand = CardBrand.CartesBancaires))

            eventReporter.assertUpdatePaymentMethodSucceededCalls(CardBrand.CartesBancaires)
        }
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

        runScenario(customerRepository = customerRepository) {
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
                cardUpdateParams = CardUpdateParams(
                    cardBrand = CardBrand.CartesBancaires
                ),
            )

            assertThat(calledUpdate.awaitItem()).isTrue()

            val paymentMethods = customerStateHolder.paymentMethods.value
            assertThat(paymentMethods).hasSize(1)
            assertThat(paymentMethods.first().card?.brand).isEqualTo(CardBrand.CartesBancaires)
            eventReporter.assertUpdatePaymentMethodSucceededCalls(CardBrand.CartesBancaires)
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `modifyCardPaymentMethod analytics events received`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
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

            savedPaymentMethodMutator.modifyCardPaymentMethod(
                paymentMethod = displayableSavedPaymentMethod.paymentMethod,
                cardUpdateParams = CardUpdateParams(cardBrand = CardBrand.CartesBancaires)
            )

            eventReporter.assertUpdatePaymentMethodSucceededCalls(CardBrand.CartesBancaires)
        }
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
            updatePaymentMethodTurbine.awaitItem()
                .updateExecutor(CardUpdateParams(cardBrand = CardBrand.CartesBancaires))

            assertThat(calledUpdate.awaitItem()).isTrue()

            val paymentMethods = customerStateHolder.paymentMethods.value
            assertThat(paymentMethods).hasSize(1)
            assertThat(paymentMethods.first().card?.brand).isEqualTo(CardBrand.Unknown)
            eventReporter.assertUpdatePaymentMethodFailedCalls(CardBrand.CartesBancaires)
        }

        calledUpdate.ensureAllEventsConsumed()
    }

    @Test
    fun `updatePaymentMethod updateExecutor failure analytics events received`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        val customerRepository = FakeCustomerRepository(
            onUpdatePaymentMethod = {
                Result.failure(IllegalStateException("Test failure"))
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
            updatePaymentMethodTurbine.awaitItem()
                .updateExecutor(CardUpdateParams(cardBrand = CardBrand.CartesBancaires))

            eventReporter.assertUpdatePaymentMethodFailedCalls(CardBrand.CartesBancaires)
        }
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
            eventReporter.assertAsDefaultPaymentMethodSucceededCalls(paymentMethods)
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
            eventReporter.assertAsDefaultPaymentMethodSucceededCalls(paymentMethods)
        }
    }

    @Test
    fun `setDefaultPaymentMethod success analytics events received`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val customerRepository = FakeCustomerRepository(
            onSetDefaultPaymentMethod = {
                Result.success(mock())
            }
        )

        runScenario(customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = paymentMethods.first().id,
                )
            )

            savedPaymentMethodMutator.setDefaultPaymentMethod(paymentMethods[1])
            eventReporter.assertAsDefaultPaymentMethodSucceededCalls(paymentMethods)
        }
    }

    @Test
    fun `setDefaultPaymentMethod failed analytics events received`() {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val customerRepository = FakeCustomerRepository(
            onSetDefaultPaymentMethod = {
                Result.failure(IllegalStateException("Test failure"))
            }
        )

        val eventReporter = FakeEventReporter()

        runScenario(eventReporter = eventReporter, customerRepository = customerRepository) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = paymentMethods.first().id,
                )
            )

            savedPaymentMethodMutator.setDefaultPaymentMethod(paymentMethods[1])

            val failedCall = eventReporter.setAsDefaultPaymentMethodFailedCalls.awaitItem()
            assertThat(failedCall.error.message).isEqualTo("Test failure")
            assertThat(failedCall.paymentMethodType).isNotNull()
            assertThat(failedCall.paymentMethodType).isEqualTo(paymentMethods[1].type?.code)
        }
    }

    @Test
    fun `defaultPaymentMethodId correctly set when isPaymentMethodSetAsDefaultEnabled`() {
        runScenarioForTestingDefaultPaymentMethod(
            initialDefaultPaymentMethodIndex = 0,
            isPaymentMethodSetAsDefaultEnabled = true,
        ) { _, savedPaymentMethodMutator, paymentMethods ->
            assertThat(savedPaymentMethodMutator.defaultPaymentMethodId.value).isEqualTo(paymentMethods.first().id)
        }
    }

    @Test
    fun `defaultPaymentMethodId correctly set when isPaymentMethodSetAsDefaultEnabled = false`() {
        runScenarioForTestingDefaultPaymentMethod(
            initialDefaultPaymentMethodIndex = 0,
            isPaymentMethodSetAsDefaultEnabled = false,
        ) { _, savedPaymentMethodMutator, _ ->
            assertThat(savedPaymentMethodMutator.defaultPaymentMethodId.value).isEqualTo(null)
        }
    }

    @Test
    fun `defaultPaymentMethodId correctly set as null when no defaultPaymentMethodId`() {
        runScenarioForTestingDefaultPaymentMethod(
            initialDefaultPaymentMethodIndex = null,
            isPaymentMethodSetAsDefaultEnabled = true,
        ) { _, savedPaymentMethodMutator, _ ->
            assertThat(savedPaymentMethodMutator.defaultPaymentMethodId.value).isEqualTo(null)
        }
    }

    @Test
    fun `defaultPaymentMethodId changes correctly when set for the first time`() {
        runScenarioForTestingDefaultPaymentMethod(
            initialDefaultPaymentMethodIndex = null,
            isPaymentMethodSetAsDefaultEnabled = true,
        ) { customerStateHolder, savedPaymentMethodMutator, paymentMethods ->
            customerStateHolder.setDefaultPaymentMethod(paymentMethods[1])

            assertThat(savedPaymentMethodMutator.defaultPaymentMethodId.value).isEqualTo(paymentMethods[1].id)
        }
    }

    @Test
    fun `defaultPaymentMethodId changes correctly when new defaultPaymentMethod set`() {
        runScenarioForTestingDefaultPaymentMethod(
            initialDefaultPaymentMethodIndex = 0,
            isPaymentMethodSetAsDefaultEnabled = true,
        ) { customerStateHolder, savedPaymentMethodMutator, paymentMethods ->
            assertThat(savedPaymentMethodMutator.defaultPaymentMethodId.value).isEqualTo(paymentMethods.first().id)

            customerStateHolder.setDefaultPaymentMethod(paymentMethods[1])

            assertThat(savedPaymentMethodMutator.defaultPaymentMethodId.value).isEqualTo(paymentMethods[1].id)
        }
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
                        canUpdateFullPaymentMethodDetails = true
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

    private fun runScenarioForTestingDefaultPaymentMethod(
        initialDefaultPaymentMethodIndex: Int?,
        isPaymentMethodSetAsDefaultEnabled: Boolean,
        block: (
            CustomerStateHolder,
            SavedPaymentMethodMutator,
            List<PaymentMethod>,
        ) -> Unit
    ) {
        val paymentMethods = PaymentMethodFixtures.createCards(3)

        val customerStateHolder = CustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = MutableStateFlow(null),
        )
        val paymentMethodMetadataFlow = stateFlowOf(
            PaymentMethodMetadataFactory.create(
                isPaymentMethodSetAsDefaultEnabled = isPaymentMethodSetAsDefaultEnabled
            )
        )
        runScenario(
            customerStateHolder = customerStateHolder,
            paymentMethodMetadataFlow = paymentMethodMetadataFlow,
        ) {
            customerStateHolder.setCustomerState(
                createCustomerState(
                    paymentMethods = paymentMethods,
                    defaultPaymentMethodId = initialDefaultPaymentMethodIndex?.let {
                        paymentMethods.getOrNull(it)?.id
                    },
                )
            )

            block(customerStateHolder, savedPaymentMethodMutator, paymentMethods)
        }
    }

    @Suppress("LongMethod")
    private fun runScenario(
        customerRepository: CustomerRepository = FakeCustomerRepository(),
        eventReporter: FakeEventReporter = FakeEventReporter(),
        selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null),
        customerStateHolder: CustomerStateHolder = CustomerStateHolder(
            savedStateHandle = SavedStateHandle(),
            selection = selection,
        ),
        paymentMethodMetadataFlow: StateFlow<PaymentMethodMetadata?> = stateFlowOf(
            PaymentMethodMetadataFactory.create()
        ),
        block: suspend Scenario.() -> Unit
    ) {
        runTest {
            val currentScreen: MutableStateFlow<PaymentSheetScreen> = MutableStateFlow(PaymentSheetScreen.Loading)

            val postPaymentMethodRemovedTurbine = Turbine<Unit>()
            val prePaymentMethodRemovedTurbine = Turbine<Unit>()
            val updatePaymentMethodTurbine = Turbine<UpdateCall>()

            val savedPaymentMethodMutator = SavedPaymentMethodMutator(
                paymentMethodMetadataFlow = paymentMethodMetadataFlow,
                eventReporter = eventReporter,
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
                        setDefaultPaymentMethodExecutor,
                    ->
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
                eventReporter = eventReporter,
            ).apply {
                block()
            }

            advanceUntilIdle()

            postPaymentMethodRemovedTurbine.ensureAllEventsConsumed()
            updatePaymentMethodTurbine.ensureAllEventsConsumed()
            eventReporter.validate()
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
        val eventReporter: FakeEventReporter,
    )

    private data class UpdateCall(
        val paymentMethod: DisplayableSavedPaymentMethod,
        val canRemove: Boolean,
        val performRemove: suspend () -> Throwable?,
        val updateExecutor: suspend (cardUpdateParams: CardUpdateParams) -> Result<PaymentMethod>,
        val setSetDefaultPaymentMethodExecutor: suspend (paymentMethod: PaymentMethod) -> Result<Unit>,
    )

    private suspend fun FakeEventReporter.assertAsDefaultPaymentMethodSucceededCalls(
        paymentMethods: List<PaymentMethod>
    ) {
        val succeededCall = setAsDefaultPaymentMethodSucceededCalls.awaitItem()
        assertThat(succeededCall).isInstanceOf(FakeEventReporter.SetAsDefaultPaymentMethodSucceededCall::class.java)
        assertThat(succeededCall.paymentMethodType).isNotNull()
        assertThat(succeededCall.paymentMethodType).isEqualTo(paymentMethods[1].type?.code)
    }

    private suspend fun FakeEventReporter.assertUpdatePaymentMethodSucceededCalls(cardBrand: CardBrand) {
        val succeededCall = updatePaymentMethodSucceededCalls.awaitItem()
        assertThat(succeededCall.selectedBrand).isEqualTo(cardBrand)
    }

    private suspend fun FakeEventReporter.assertUpdatePaymentMethodFailedCalls(cardBrand: CardBrand) {
        val failedCall = updatePaymentMethodFailedCalls.awaitItem()
        assertThat(failedCall.selectedBrand).isEqualTo(cardBrand)
        assertThat(failedCall.error.message).isEqualTo("Test failure")
    }
}
