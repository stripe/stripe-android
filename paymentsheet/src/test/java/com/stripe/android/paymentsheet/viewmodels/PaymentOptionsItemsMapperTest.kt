package com.stripe.android.paymentsheet.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.core.strings.resolvableString
import com.stripe.android.lpmfoundations.paymentmethod.CustomerMetadata
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.state.CustomerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PaymentOptionsItemsMapperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val customerStateFlow = MutableStateFlow<CustomerState?>(null)
    private val isGooglePayReadyFlow = MutableStateFlow(false)
    private val isLinkEnabledFlow = MutableStateFlow<Boolean?>(null)

    @Test
    fun `Only emits value if required flows have emitted values`() = runTest {
        val mapper = PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = { false },
            customerMetadata = MutableStateFlow(
                CustomerMetadata(
                    hasCustomerConfiguration = false,
                    isPaymentMethodSetAsDefaultEnabled = false
                )
            ),
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())

            customerStateFlow.value = createCustomerState(
                paymentMethods = PaymentMethodFixtures.createCards(2)
            )
            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            val state = awaitItem()
            assertThat(state).hasSize(5)
            assertThat(state[0].viewType).isEqualTo(PaymentOptionsItem.ViewType.AddCard)
            assertThat(state[1].viewType).isEqualTo(PaymentOptionsItem.ViewType.GooglePay)
            assertThat(state[2].viewType).isEqualTo(PaymentOptionsItem.ViewType.Link)
            assertThat(state[3].viewType).isEqualTo(PaymentOptionsItem.ViewType.SavedPaymentMethod)
            assertThat(state[4].viewType).isEqualTo(PaymentOptionsItem.ViewType.SavedPaymentMethod)
        }
    }

    @Test
    fun `Doesn't include Google Pay and Link in payment flow`() = runTest {
        val mapper = PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = { false },
            customerMetadata = MutableStateFlow(
                CustomerMetadata(
                    hasCustomerConfiguration = false,
                    isPaymentMethodSetAsDefaultEnabled = false
                )
            ),
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())

            customerStateFlow.value = createCustomerState(
                paymentMethods = PaymentMethodFixtures.createCards(2)
            )
            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            assertThat(awaitItem()).containsNoneOf(
                PaymentOptionsItem.GooglePay,
                PaymentOptionsItem.Link,
            )
        }
    }


    @Test
    fun `Correctly creates payment methods in paymentFlow when not isPaymentMethodSetAsDefaultEnabled`() = runTest {
        val mapper = PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = { false },
            customerMetadata = MutableStateFlow(
                CustomerMetadata(
                    hasCustomerConfiguration = false,
                    isPaymentMethodSetAsDefaultEnabled = false
                )
            ),
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())
            val paymentMethods = PaymentMethodFixtures.createCards(2)
            val defaultPaymentMethodId = paymentMethods[1].id

            customerStateFlow.value = createCustomerState(
                paymentMethods = paymentMethods,
                defaultPaymentMethodId = defaultPaymentMethodId
            )

            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            val state = awaitItem()
            assertThat(state).hasSize(3)
            assertThat(state[0].viewType).isEqualTo(PaymentOptionsItem.ViewType.AddCard)
            assertThat(
                (state[1] as? PaymentOptionsItem.SavedPaymentMethod)
                    ?.displayableSavedPaymentMethod?.shouldShowDefaultBadge).isFalse()
            assertThat(
                (state[2] as? PaymentOptionsItem.SavedPaymentMethod)
                    ?.displayableSavedPaymentMethod?.shouldShowDefaultBadge).isFalse()
        }
    }

    @Test
    fun `Correctly creates payment methods in paymentFlow when isPaymentMethodSetAsDefaultEnabled`() = runTest {
        val mapper = PaymentOptionsItemsMapper(
            customerState = customerStateFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
            nameProvider = { it!!.resolvableString },
            isCbcEligible = { false },
            customerMetadata = MutableStateFlow(
                CustomerMetadata(
                    hasCustomerConfiguration = false,
                    isPaymentMethodSetAsDefaultEnabled = true
                )
            ),
        )

        mapper().test {
            assertThat(awaitItem()).isEqualTo(emptyList<PaymentOptionsItem>())
            val paymentMethods = PaymentMethodFixtures.createCards(2)
            val defaultPaymentMethodId = paymentMethods[1].id

            customerStateFlow.value = createCustomerState(
                paymentMethods = paymentMethods,
                defaultPaymentMethodId = defaultPaymentMethodId
            )

            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            val state = awaitItem()
            assertThat(state).hasSize(3)
            assertThat(state[0].viewType).isEqualTo(PaymentOptionsItem.ViewType.AddCard)
            assertThat(
                (state[1] as? PaymentOptionsItem.SavedPaymentMethod)
                    ?.displayableSavedPaymentMethod?.shouldShowDefaultBadge).isFalse()
            assertThat(
                (state[2] as? PaymentOptionsItem.SavedPaymentMethod)
                    ?.displayableSavedPaymentMethod?.shouldShowDefaultBadge).isTrue()
        }
    }

    private fun createCustomerState(
        defaultPaymentMethodId: String? = null,
        paymentMethods: List<PaymentMethod> = emptyList(),
    ): CustomerState {
        return CustomerState(
            id = "pi_123",
            ephemeralKeySecret = "ek_123",
            customerSessionClientSecret = null,
            paymentMethods = paymentMethods,
            permissions = CustomerState.Permissions(
                canRemovePaymentMethods = true,
                canRemoveLastPaymentMethod = true,
                canRemoveDuplicates = false,
            ),
            defaultPaymentMethodId = defaultPaymentMethodId,
        )
    }
}
