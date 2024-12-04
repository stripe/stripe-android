package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class CustomerStateHolderTest {
    @Test
    fun `customer is initialized as null`() = runScenario {
        customerStateHolder.customer.test {
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `paymentMethod is initialized as emptyList`() = runScenario {
        customerStateHolder.paymentMethods.test {
            assertThat(awaitItem()).isEmpty()
        }
    }

    @Test
    fun `customer is restored from savedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_123",
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
            paymentMethods = emptyList()
        )
        savedStateHandle[CustomerStateHolder.SAVED_CUSTOMER] = customerState
        runScenario(savedStateHandle = savedStateHandle) {
            customerStateHolder.customer.test {
                assertThat(awaitItem()).isEqualTo(customerState)
            }
        }
    }

    @Test
    fun `paymentMethods emit once customer is updated`() = runScenario {
        customerStateHolder.paymentMethods.test {
            assertThat(awaitItem()).isEmpty()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                )
            )

            assertThat(awaitItem()).hasSize(1)
        }
    }

    private fun runScenario(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        selection: StateFlow<PaymentSelection?> = stateFlowOf(null),
        block: suspend Scenario.() -> Unit
    ) {
        val customerStateHolder = CustomerStateHolder(
            savedStateHandle = savedStateHandle,
            selection = selection,
        )
        Scenario(customerStateHolder).apply {
            runTest {
                block()
            }
        }
    }

    private data class Scenario(
        val customerStateHolder: CustomerStateHolder,
    )
}
