package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.paymentsheet.state.GooglePayState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.Mockito.mock

class SavedPaymentMethodMutatorTest {
    @Test
    fun `customer is initialized as null`() = runScenario {
        assertThat(savedPaymentMethodMutator.customer).isNull()
    }

    @Test
    fun `customer is restored from savedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_123",
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
            paymentMethods = emptyList()
        )
        savedStateHandle[SavedPaymentMethodMutator.SAVED_CUSTOMER] = customerState
        runScenario(savedStateHandle = savedStateHandle) {
            assertThat(savedPaymentMethodMutator.customer).isEqualTo(customerState)
        }
    }

    @Test
    fun `paymentMethods emit once customer is updated`() = runScenario {
        savedPaymentMethodMutator.paymentMethods.test {
            assertThat(awaitItem()).isEmpty()

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            )

            assertThat(awaitItem()).hasSize(1)
        }
    }

    @Test
    fun `canEdit is correct when allowsRemovalOfLastSavedPaymentMethod is true`() = runScenario(
        allowsRemovalOfLastSavedPaymentMethod = true,
    ) {
        savedPaymentMethodMutator.canEdit.test {
            assertThat(awaitItem()).isFalse()

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
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

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_PAYMENT_METHOD,
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                )
            )
            assertThat(awaitItem()).isTrue()

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                )
            )
            assertThat(awaitItem()).isFalse()

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
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

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD,
                )
            )
            assertThat(awaitItem()).isTrue()

            savedPaymentMethodMutator.customer = null
            assertThat(awaitItem()).isFalse()

            savedPaymentMethodMutator.customer = CustomerState.createForLegacyEphemeralKey(
                customerId = "cus_123",
                accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                paymentMethods = listOf(
                    PaymentMethodFixtures.CARD_WITH_NETWORKS_PAYMENT_METHOD
                )
            )
            assertThat(awaitItem()).isTrue()
        }
    }

    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        allowsRemovalOfLastSavedPaymentMethod: Boolean = true,
        isCbcEligible: () -> Boolean = { false },
        block: suspend Scenario.() -> Unit
    ) {
        runTest {
            val savedPaymentMethodMutator = SavedPaymentMethodMutator(
                editInteractorFactory = mock(),
                eventReporter = mock(),
                savedStateHandle = savedStateHandle,
                coroutineScope = CoroutineScope(UnconfinedTestDispatcher()),
                workContext = coroutineContext,
                navigationHandler = mock(),
                customerRepository = mock(),
                allowsRemovalOfLastSavedPaymentMethod = allowsRemovalOfLastSavedPaymentMethod,
                selection = stateFlowOf(null),
                providePaymentMethodName = { it.orEmpty() },
                addFirstPaymentMethodScreenFactory = { throw AssertionError("Not implemented") },
                updateSelection = { throw AssertionError("Not implemented") },
                isCbcEligible = isCbcEligible,
                googlePayState = stateFlowOf(GooglePayState.NotAvailable),
                isLinkEnabled = stateFlowOf(false),
                isNotPaymentFlow = true,
            )
            Scenario(savedPaymentMethodMutator).apply {
                block()
            }
        }
    }

    private data class Scenario(
        val savedPaymentMethodMutator: SavedPaymentMethodMutator,
    )
}
