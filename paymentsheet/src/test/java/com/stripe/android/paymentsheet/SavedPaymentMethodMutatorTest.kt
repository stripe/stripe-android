package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.DefaultCardBrandFilter
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.model.PaymentMethodFixtures.toDisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.verticalmode.FakeManageScreenInteractor
import com.stripe.android.paymentsheet.verticalmode.PaymentMethodVerticalLayoutInteractor
import com.stripe.android.testing.PaymentMethodFactory
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class SavedPaymentMethodMutatorTest {
    @Test
    fun `canRemove is correct when no payment methods for customer`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = true,
    ) {
        savedPaymentMethodMutator.canRemove.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf()
                )
            )

            // Should still be false so expect no more events
            expectNoEvents()
        }
    }

    @Test
    fun `canRemove is correct when one payment method & allowsRemovalOfLastSavedPaymentMethod is true`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    CustomerState.createForLegacyEphemeralKey(
                        customerId = "cus_123",
                        accessType = PaymentSheet
                            .CustomerAccessType
                            .LegacyCustomerEphemeralKey("ek_123"),
                        paymentMethods = PaymentMethodFactory.cards(1),
                    )
                )

                assertThat(awaitItem()).isTrue()

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canRemove is correct when one payment method & allowsRemovalOfLastSavedPaymentMethod is false`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    CustomerState.createForLegacyEphemeralKey(
                        customerId = "cus_123",
                        accessType = PaymentSheet
                            .CustomerAccessType
                            .LegacyCustomerEphemeralKey("ek_123"),
                        paymentMethods = PaymentMethodFactory.cards(1),
                    )
                )

                // Should still be false so expect no more events
                expectNoEvents()
            }
        }

    @Test
    fun `canRemove is correct when multiple payment methods & allowsRemovalOfLastSavedPaymentMethod is true`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    CustomerState.createForLegacyEphemeralKey(
                        customerId = "cus_123",
                        accessType = PaymentSheet
                            .CustomerAccessType
                            .LegacyCustomerEphemeralKey("ek_123"),
                        paymentMethods = PaymentMethodFactory.cards(2),
                    )
                )

                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `canRemove is correct when multiple payment methods & allowsRemovalOfLastSavedPaymentMethod is false`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    CustomerState.createForLegacyEphemeralKey(
                        customerId = "cus_123",
                        accessType = PaymentSheet
                            .CustomerAccessType
                            .LegacyCustomerEphemeralKey("ek_123"),
                        paymentMethods = PaymentMethodFactory.cards(2),
                    )
                )

                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `canRemove is correct when has remove permissions & allowsRemovalOfLastSavedPaymentMethod is true`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        paymentMethods = PaymentMethodFactory.cards(1),
                        isRemoveEnabled = true,
                    )
                )

                assertThat(awaitItem()).isTrue()

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canRemove is correct when has remove permissions & allowsRemovalOfLastSavedPaymentMethod is false`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = false,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        paymentMethods = PaymentMethodFactory.cards(1),
                        isRemoveEnabled = true,
                    )
                )

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canRemove is correct when does not remove permissions & allowsRemovalOfLastSavedPaymentMethod is true`() =
        runScenario(
            allowsRemovalOfLastSavedPaymentMethod = true,
        ) {
            savedPaymentMethodMutator.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        paymentMethods = PaymentMethodFactory.cards(1),
                        isRemoveEnabled = false,
                    )
                )

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canEdit is correct when no payment methods`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = true,
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet
                        .CustomerAccessType
                        .LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf()
                )
            )

            // Should still be false so expect no more events
            expectNoEvents()
        }
    }

    @Test
    fun `canEdit is correct when allowsRemovalOfLastSavedPaymentMethod is true`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = true,
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                )
            )
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `canEdit is correct when allowsRemovalOfLastSavedPaymentMethod is false`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = false,
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                    )
                )
            )
            assertThat(awaitItem()).isTrue()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
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
        allowsRemovalOfLastSavedPaymentMethod = false,
        isCbcEligible = { true }
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                    )
                )
            )
            assertThat(awaitItem()).isTrue()

            customerStateHolder.setCustomerState(null)
            assertThat(awaitItem()).isFalse()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                    )
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
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD
                    )
                )
            )

            customerStateHolder.paymentMethods.test {
                assertThat(awaitItem().size).isEqualTo(1)
                savedPaymentMethodMutator.removePaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                assertThat(awaitItem()).isEmpty()
            }

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
    }

    @Test
    fun `modifyPaymentMethod should create modify screen correctly when can remove`() = runScenario {
        val cards = PaymentMethodFixtures.createCards(3)

        customerStateHolder.setCustomerState(
            createCustomerState(
                paymentMethods = cards,
                isRemoveEnabled = true,
            )
        )

        savedPaymentMethodMutator.modifyPaymentMethod(cards[0])

        val call = editPaymentMethodInteractorFactory.calls.awaitItem()

        assertThat(call.canRemove).isTrue()
    }

    @Test
    fun `modifyPaymentMethod should create modify screen correctly when cannot remove`() = runScenario {
        val cards = PaymentMethodFixtures.createCards(3)

        customerStateHolder.setCustomerState(
            createCustomerState(
                paymentMethods = cards,
                isRemoveEnabled = false,
            )
        )

        savedPaymentMethodMutator.modifyPaymentMethod(cards[0])

        val call = editPaymentMethodInteractorFactory.calls.awaitItem()

        assertThat(call.canRemove).isFalse()
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
    }

    @Test
    fun `Exiting the manage saved PMs screen resets editing to false`() {
        runScenario {
            currentScreen.value = PaymentSheetScreen.ManageSavedPaymentMethods(
                interactor = FakeManageScreenInteractor()
            )

            savedPaymentMethodMutator.toggleEditing()
            savedPaymentMethodMutator.editing.test {
                assertThat(awaitItem()).isTrue()
            }

            currentScreen.value = PaymentSheetScreen.VerticalMode(
                interactor = FakePaymentMethodVerticalLayoutInteractor
            )

            savedPaymentMethodMutator.editing.test {
                assertThat(awaitItem()).isFalse()
            }
        }
    }

    private object FakePaymentMethodVerticalLayoutInteractor : PaymentMethodVerticalLayoutInteractor {
        override val isLiveMode: Boolean = false
        override val state: StateFlow<PaymentMethodVerticalLayoutInteractor.State> = mock()
        override val showsWalletsHeader: StateFlow<Boolean> = MutableStateFlow(false)

        override fun handleViewAction(viewAction: PaymentMethodVerticalLayoutInteractor.ViewAction) {
            // Do nothing.
        }
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
    fun `updatePaymentMethod for card navigates to update payment method screen`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.cards(1).first().toDisplayableSavedPaymentMethod()
        runScenario {
            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            verify(navigationHandler).transitionTo(any())
        }
    }

    @Test
    fun `updatePaymentMethod for unsupported SPM type does nothing`() {
        val displayableSavedPaymentMethod = PaymentMethodFactory.amazonPay().toDisplayableSavedPaymentMethod()
        runScenario {
            savedPaymentMethodMutator.updatePaymentMethod(displayableSavedPaymentMethod)

            verifyNoInteractions(navigationHandler)
        }
    }

    private fun createCustomerState(
        paymentMethods: List<PaymentMethod>,
        isRemoveEnabled: Boolean,
    ): CustomerState {
        return CustomerState(
            id = "cus_1",
            ephemeralKeySecret = "ek_1",
            paymentMethods = paymentMethods,
            permissions = CustomerState.Permissions(
                canRemovePaymentMethods = isRemoveEnabled,
                canRemoveDuplicates = true,
            )
        )
    }

    private fun removeDuplicatesTest(shouldRemoveDuplicates: Boolean) {
        val repository = FakeCustomerRepository()

        runScenario(repository) {
            customerStateHolder.setCustomerState(
                CustomerState(
                    id = "cus_1",
                    ephemeralKeySecret = "ek_1",
                    paymentMethods = listOf(),
                    permissions = CustomerState.Permissions(
                        canRemovePaymentMethods = true,
                        canRemoveDuplicates = shouldRemoveDuplicates,
                    )
                )
            )

            val paymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD

            savedPaymentMethodMutator.removePaymentMethod(paymentMethod)

            assertThat(repository.detachRequests.awaitItem()).isEqualTo(
                FakeCustomerRepository.DetachRequest(
                    paymentMethodId = paymentMethod.id!!,
                    customerInfo = CustomerRepository.CustomerInfo(
                        id = "cus_1",
                        ephemeralKeySecret = "ek_1",
                    ),
                    canRemoveDuplicates = shouldRemoveDuplicates,
                )
            )
        }
    }

    private fun runScenario(
        customerRepository: CustomerRepository = FakeCustomerRepository(),
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        isCbcEligible: () -> Boolean = { false },
        block: suspend Scenario.() -> Unit
    ) {
        runTest {
            val selection: MutableStateFlow<PaymentSelection?> = MutableStateFlow(null)
            val currentScreen: MutableStateFlow<PaymentSheetScreen> = MutableStateFlow(PaymentSheetScreen.Loading)

            val customerStateHolder = CustomerStateHolder(
                savedStateHandle = SavedStateHandle(),
                selection = selection,
            )
            val navigationHandler = mock<NavigationHandler>()
            val editPaymentMethodInteractorFactory = FakeEditPaymentMethodInteractor.Factory()
            whenever(navigationHandler.currentScreen).thenReturn(stateFlowOf(PaymentSheetScreen.Loading))
            val savedPaymentMethodMutator = SavedPaymentMethodMutator(
                editInteractorFactory = editPaymentMethodInteractorFactory,
                eventReporter = mock(),
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                workContext = coroutineContext,
                navigationHandler = navigationHandler,
                customerRepository = customerRepository,
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
                selection = selection,
                providePaymentMethodName = { it?.resolvableString.orEmpty() },
                customerStateHolder = customerStateHolder,
                addFirstPaymentMethodScreenFactory = { throw AssertionError("Not implemented") },
                clearSelection = { selection.value = null },
                isCbcEligible = isCbcEligible,
                isGooglePayReady = stateFlowOf(false),
                isLinkEnabled = stateFlowOf(false),
                isNotPaymentFlow = true,
                isLiveModeProvider = { true },
                currentScreen = currentScreen,
                cardBrandFilter = DefaultCardBrandFilter
            )
            Scenario(
                savedPaymentMethodMutator = savedPaymentMethodMutator,
                customerStateHolder = customerStateHolder,
                selectionSource = selection,
                editPaymentMethodInteractorFactory = editPaymentMethodInteractorFactory,
                currentScreen = currentScreen,
                navigationHandler = navigationHandler,
            ).apply {
                block()
            }
        }
    }

    private data class Scenario(
        val savedPaymentMethodMutator: SavedPaymentMethodMutator,
        val customerStateHolder: CustomerStateHolder,
        val selectionSource: MutableStateFlow<PaymentSelection?>,
        val editPaymentMethodInteractorFactory: FakeEditPaymentMethodInteractor.Factory,
        val currentScreen: MutableStateFlow<PaymentSheetScreen>,
        val navigationHandler: NavigationHandler,
    )
}
