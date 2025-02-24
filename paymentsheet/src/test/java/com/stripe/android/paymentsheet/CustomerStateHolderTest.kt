package com.stripe.android.paymentsheet

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.common.model.asCommonConfiguration
import com.stripe.android.model.CardBrand
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.CustomerState
import com.stripe.android.testing.PaymentMethodFactory
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

    @Test
    fun `MostRecentlySelectedSavedPaymentMethod is restored from savedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val customerState = CustomerState.createForLegacyEphemeralKey(
            customerId = "cus_123",
            configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
            accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
            paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
        )
        savedStateHandle[CustomerStateHolder.SAVED_CUSTOMER] = customerState
        savedStateHandle[CustomerStateHolder.SAVED_PM_SELECTION] = PaymentMethodFixtures.CARD_PAYMENT_METHOD
        runScenario(savedStateHandle = savedStateHandle) {
            customerStateHolder.customer.test {
                assertThat(awaitItem()).isEqualTo(customerState)
            }
            customerStateHolder.paymentMethods.test {
                assertThat(awaitItem()).containsExactly(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            }
            customerStateHolder.mostRecentlySelectedSavedPaymentMethod.test {
                assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            }
        }
    }

    @Test
    fun `MostRecentlySelectedSavedPaymentMethod is cleared if paymentMethod no longer exists`() = runScenario {
        customerStateHolder.mostRecentlySelectedSavedPaymentMethod.test {
            assertThat(awaitItem()).isNull()

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
                )
            )
            customerStateHolder.updateMostRecentlySelectedSavedPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = emptyList(),
                )
            )
            assertThat(awaitItem()).isNull()
        }
    }

    @Test
    fun `MostRecentlySelectedSavedPaymentMethod is maintained if paymentMethod continues to exists`() = runScenario {
        customerStateHolder.mostRecentlySelectedSavedPaymentMethod.test {
            assertThat(awaitItem()).isNull()

            val extraPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_333")
            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD, extraPaymentMethod)
                )
            )
            customerStateHolder.updateMostRecentlySelectedSavedPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            assertThat(awaitItem()).isEqualTo(PaymentMethodFixtures.CARD_PAYMENT_METHOD)

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD),
                )
            )
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `MostRecentlySelectedSavedPaymentMethod is updated if customer state updates payment method`() = runScenario {
        customerStateHolder.mostRecentlySelectedSavedPaymentMethod.test {
            assertThat(awaitItem()).isNull()

            val extraPaymentMethod = PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(id = "pm_333")
            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(PaymentMethodFixtures.CARD_PAYMENT_METHOD, extraPaymentMethod)
                )
            )
            customerStateHolder.updateMostRecentlySelectedSavedPaymentMethod(PaymentMethodFixtures.CARD_PAYMENT_METHOD)
            assertThat(awaitItem()?.card?.brand).isEqualTo(CardBrand.Visa)

            customerStateHolder.setCustomerState(
                CustomerState.createForLegacyEphemeralKey(
                    customerId = "cus_123",
                    configuration = PaymentSheetFixtures.CONFIG_CUSTOMER.asCommonConfiguration(),
                    accessType = PaymentSheet.CustomerAccessType.LegacyCustomerEphemeralKey("ek_123"),
                    paymentMethods = listOf(
                        PaymentMethodFixtures.CARD_PAYMENT_METHOD.copy(
                            card = PaymentMethodFixtures.CARD_PAYMENT_METHOD.card?.copy(
                                brand = CardBrand.CartesBancaires
                            )
                        )
                    ),
                )
            )
            assertThat(awaitItem()?.card?.brand).isEqualTo(CardBrand.CartesBancaires)
        }
    }

    @Test
    fun `canRemove is correct when no payment methods for customer`() = runScenario {
        customerStateHolder.canRemove.test {
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
    fun `canRemove is correct when one payment method & can remove last payment method`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        isRemoveEnabled = true,
                        canRemoveLastPaymentMethod = true,
                        paymentMethods = PaymentMethodFactory.cards(1),
                    )
                )

                assertThat(awaitItem()).isTrue()

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canRemove is correct when one payment method & cannot remove last payment method`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        isRemoveEnabled = true,
                        canRemoveLastPaymentMethod = false,
                        paymentMethods = PaymentMethodFactory.cards(1),
                    )
                )

                // Should still be false so expect no more events
                expectNoEvents()
            }
        }

    @Test
    fun `canRemove is correct when multiple payment methods & can remove last payment method`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        isRemoveEnabled = true,
                        canRemoveLastPaymentMethod = true,
                        paymentMethods = PaymentMethodFactory.cards(2),
                    )
                )

                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `canRemove is correct when multiple payment methods & cannot remove last payment method`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        isRemoveEnabled = true,
                        canRemoveLastPaymentMethod = false,
                        paymentMethods = PaymentMethodFactory.cards(2),
                    )
                )

                assertThat(awaitItem()).isTrue()
            }
        }

    @Test
    fun `canRemove is correct when has remove permissions & can remove last payment method`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        paymentMethods = PaymentMethodFactory.cards(1),
                        isRemoveEnabled = true,
                        canRemoveLastPaymentMethod = true,
                    )
                )

                assertThat(awaitItem()).isTrue()

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canRemove is correct when has remove permissions & canRemoveLastPaymentMethod is false`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        paymentMethods = PaymentMethodFactory.cards(1),
                        isRemoveEnabled = true,
                        canRemoveLastPaymentMethod = false,
                    )
                )

                ensureAllEventsConsumed()
            }
        }

    @Test
    fun `canRemove is correct when does not remove permissions & canRemoveLastPaymentMethod is true`() =
        runScenario {
            customerStateHolder.canRemove.test {
                assertThat(awaitItem()).isFalse()

                customerStateHolder.setCustomerState(
                    createCustomerState(
                        paymentMethods = PaymentMethodFactory.cards(1),
                        isRemoveEnabled = false,
                        canRemoveLastPaymentMethod = true,
                    )
                )

                ensureAllEventsConsumed()
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
