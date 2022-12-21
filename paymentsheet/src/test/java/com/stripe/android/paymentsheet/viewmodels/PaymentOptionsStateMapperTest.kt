package com.stripe.android.paymentsheet.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentOptionsStateMapperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val paymentMethodsFlow = MutableStateFlow<List<PaymentMethod>>(listOf())
    private val initialSelectionFlow = MutableStateFlow<SavedSelection>(SavedSelection.None)
    private val currentSelectionFlow = MutableStateFlow<PaymentSelection?>(null)
    private val isGooglePayReadyFlow = MutableStateFlow<Boolean>(false)
    private val isLinkEnabledFlow = MutableStateFlow<Boolean>(false)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanup() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Doesn't include Google Pay and Link in payment flow`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            initialSelection = initialSelectionFlow,
            currentSelection = currentSelectionFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
        )

        val cards = PaymentMethodFixtures.createCards(2)
        paymentMethodsFlow.value = cards
        initialSelectionFlow.value = SavedSelection.PaymentMethod(id = cards.first().id!!)
        isGooglePayReadyFlow.value = true
        isLinkEnabledFlow.value = true

        mapper().test {
            assertThat(awaitItem().items).containsNoneOf(
                PaymentOptionsItem.GooglePay,
                PaymentOptionsItem.Link,
            )
        }
    }

    @Test
    fun `Removing selected payment option results in saved selection being selected`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            initialSelection = initialSelectionFlow,
            currentSelection = currentSelectionFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
        )

        val cards = PaymentMethodFixtures.createCards(2)
        val selectedPaymentMethod = PaymentSelection.Saved(paymentMethod = cards.last())
        paymentMethodsFlow.value = cards
        initialSelectionFlow.value = SavedSelection.Link
        currentSelectionFlow.value = selectedPaymentMethod
        isGooglePayReadyFlow.value = true
        isLinkEnabledFlow.value = true

        mapper().test {
            assertThat(awaitItem().selectedItem).isEqualTo(
                PaymentOptionsItem.SavedPaymentMethod(selectedPaymentMethod.paymentMethod)
            )
        }

        // Remove the currently selected payment option
        paymentMethodsFlow.value = cards - selectedPaymentMethod.paymentMethod
        currentSelectionFlow.value = null

        mapper().test {
            assertThat(awaitItem().selectedItem).isEqualTo(PaymentOptionsItem.Link)
        }
    }

    @Test
    fun `Removing selected payment option results in first available option if no saved selection`() =
        runTest(
            UnconfinedTestDispatcher()
        ) {
            val mapper = PaymentOptionsStateMapper(
                paymentMethods = paymentMethodsFlow,
                initialSelection = initialSelectionFlow,
                currentSelection = currentSelectionFlow,
                isGooglePayReady = isGooglePayReadyFlow,
                isLinkEnabled = isLinkEnabledFlow,
                isNotPaymentFlow = true,
            )


            val cards = PaymentMethodFixtures.createCards(2)
            val selectedPaymentMethod = PaymentSelection.Saved(paymentMethod = cards.last())
            paymentMethodsFlow.value = cards
            initialSelectionFlow.value = SavedSelection.None
            currentSelectionFlow.value = selectedPaymentMethod
            isGooglePayReadyFlow.value = true
            isLinkEnabledFlow.value = true

            mapper().test {
                assertThat(awaitItem().selectedItem).isEqualTo(
                    PaymentOptionsItem.SavedPaymentMethod(selectedPaymentMethod.paymentMethod)
                )
            }

            // Remove the currently selected payment option
            paymentMethodsFlow.value = cards - selectedPaymentMethod.paymentMethod
            currentSelectionFlow.value = null

            mapper().test {
                assertThat(awaitItem().selectedItem).isEqualTo(PaymentOptionsItem.GooglePay)
            }
        }
}
