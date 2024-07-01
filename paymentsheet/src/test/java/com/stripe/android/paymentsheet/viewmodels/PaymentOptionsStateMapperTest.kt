package com.stripe.android.paymentsheet.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.DisplayableSavedPaymentMethod
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.state.GooglePayState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PaymentOptionsStateMapperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val paymentMethodsFlow = MutableStateFlow<List<PaymentMethod>>(emptyList())
    private val currentSelectionFlow = MutableStateFlow<PaymentSelection?>(null)
    private val googlePayStateFlow = MutableStateFlow<GooglePayState>(GooglePayState.Indeterminate)
    private val isLinkEnabledFlow = MutableStateFlow<Boolean?>(null)

    @Test
    fun `Only emits value if required flows have emitted values`() = runTest {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            currentSelection = currentSelectionFlow,
            googlePayState = googlePayStateFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
            nameProvider = { it!! },
            isCbcEligible = { false }
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(PaymentOptionsState())

            paymentMethodsFlow.value = PaymentMethodFixtures.createCards(2)
            currentSelectionFlow.value = PaymentSelection.GooglePay
            googlePayStateFlow.value = GooglePayState.Available
            isLinkEnabledFlow.value = true

            val state = awaitItem()
            assertThat(state.selectedIndex).isEqualTo(1)
            assertThat(state.items).hasSize(5)
            assertThat(state.items[0].viewType).isEqualTo(PaymentOptionsItem.ViewType.AddCard)
            assertThat(state.items[1].viewType).isEqualTo(PaymentOptionsItem.ViewType.GooglePay)
            assertThat(state.items[2].viewType).isEqualTo(PaymentOptionsItem.ViewType.Link)
            assertThat(state.items[3].viewType).isEqualTo(PaymentOptionsItem.ViewType.SavedPaymentMethod)
            assertThat(state.items[4].viewType).isEqualTo(PaymentOptionsItem.ViewType.SavedPaymentMethod)
        }
    }

    @Test
    fun `Doesn't include Google Pay and Link in payment flow`() = runTest {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            currentSelection = currentSelectionFlow,
            googlePayState = googlePayStateFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
            nameProvider = { it!! },
            isCbcEligible = { false }
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(PaymentOptionsState())

            val cards = PaymentMethodFixtures.createCards(2)
            paymentMethodsFlow.value = cards
            currentSelectionFlow.value = PaymentSelection.Saved(cards.first())
            googlePayStateFlow.value = GooglePayState.Available
            isLinkEnabledFlow.value = true

            assertThat(awaitItem().items).containsNoneOf(
                PaymentOptionsItem.GooglePay,
                PaymentOptionsItem.Link,
            )
        }
    }

    @Test
    fun `Removing selected payment option results in selection being null`() = runTest {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            currentSelection = currentSelectionFlow,
            googlePayState = googlePayStateFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
            nameProvider = { it!! },
            isCbcEligible = { false }
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(PaymentOptionsState())

            val cards = PaymentMethodFixtures.createCards(2)
            val selectedPaymentMethod = PaymentSelection.Saved(paymentMethod = cards.last())
            paymentMethodsFlow.value = cards
            currentSelectionFlow.value = selectedPaymentMethod
            googlePayStateFlow.value = GooglePayState.Available
            isLinkEnabledFlow.value = true

            assertThat(awaitItem().selectedItem).isEqualTo(
                PaymentOptionsItem.SavedPaymentMethod(
                    DisplayableSavedPaymentMethod(
                        displayName = "card",
                        paymentMethod = selectedPaymentMethod.paymentMethod,
                    )
                )
            )

            // Remove the currently selected payment option
            paymentMethodsFlow.value = cards - selectedPaymentMethod.paymentMethod
            currentSelectionFlow.value = null

            assertThat(awaitItem().selectedItem).isNull()
        }
    }
}
