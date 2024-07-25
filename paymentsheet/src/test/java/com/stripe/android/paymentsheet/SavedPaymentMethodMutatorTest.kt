package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.orEmpty
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentSheetFixtures.EMPTY_CUSTOMER_STATE
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.navigation.NavigationHandler
import com.stripe.android.paymentsheet.navigation.PaymentSheetScreen
import com.stripe.android.paymentsheet.repositories.CustomerRepository
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.uicore.utils.stateFlowOf
import com.stripe.android.utils.FakeCustomerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

class SavedPaymentMethodMutatorTest {
    @Test
    fun `canEdit is correct when allowsRemovalOfLastSavedPaymentMethod is true`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = true,
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
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

            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                )
            )
            assertThat(awaitItem()).isTrue()

            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                )
            )
            assertThat(awaitItem()).isFalse()

            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf()
            )
        }
    }

    @Test
    fun `canEdit is correct CBC is enabled`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = false,
        isCbcEligible = { true }
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                )
            )
            assertThat(awaitItem()).isTrue()

            customerStateHolder.customer = null
            assertThat(awaitItem()).isFalse()

            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
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
            customerStateHolder.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD
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
        customerStateHolder.customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = customerPaymentMethods)

        savedPaymentMethodMutator.editing.test {
            assertThat(awaitItem()).isFalse()

            savedPaymentMethodMutator.toggleEditing()
            assertThat(awaitItem()).isTrue()

            savedPaymentMethodMutator.removePaymentMethod(customerPaymentMethods.single())
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `Removing selected payment method clears selection`() = runScenario {
        val cards = PaymentMethodFixtures.createCards(3)
        customerStateHolder.customer = EMPTY_CUSTOMER_STATE.copy(paymentMethods = cards)

        val selection = PaymentSelection.Saved(cards[1])
        selectionSource.value = selection

        selectionSource.test {
            assertThat(awaitItem()).isEqualTo(selection)
            savedPaymentMethodMutator.removePaymentMethod(selection.paymentMethod)
            assertThat(awaitItem()).isNull()
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

            val customerStateHolder = CustomerStateHolder(
                savedStateHandle = SavedStateHandle(),
                selection = selection,
            )
            val navigationHandler = mock<NavigationHandler>()
            whenever(navigationHandler.currentScreen).thenReturn(stateFlowOf(PaymentSheetScreen.Loading))
            val savedPaymentMethodMutator = SavedPaymentMethodMutator(
                editInteractorFactory = mock(),
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
            )
            Scenario(
                savedPaymentMethodMutator = savedPaymentMethodMutator,
                customerStateHolder = customerStateHolder,
                selectionSource = selection,
            ).apply {
                block()
            }
        }
    }

    private data class Scenario(
        val savedPaymentMethodMutator: SavedPaymentMethodMutator,
        val customerStateHolder: CustomerStateHolder,
        val selectionSource: MutableStateFlow<PaymentSelection?>,
    )
}
