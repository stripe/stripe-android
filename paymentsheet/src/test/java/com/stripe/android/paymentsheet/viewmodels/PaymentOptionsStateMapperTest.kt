package com.stripe.android.paymentsheet.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.model.PaymentMethodFixtures
import com.stripe.android.paymentsheet.PaymentOptionsItem
import com.stripe.android.paymentsheet.PaymentOptionsState
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.paymentsheet.model.SavedSelection
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PaymentOptionsStateMapperTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var paymentMethodsFlow: MutableLiveData<List<PaymentMethod>>
    private lateinit var initialSelectionFlow: MutableLiveData<SavedSelection>
    private lateinit var currentSelectionFlow: MutableLiveData<PaymentSelection?>
    private lateinit var isGooglePayReadyFlow: MutableLiveData<Boolean>
    private lateinit var isLinkEnabledFlow: MutableLiveData<Boolean>

    @Before
    fun setup() {
        paymentMethodsFlow = MutableLiveData()
        initialSelectionFlow = MutableLiveData()
        currentSelectionFlow = MutableLiveData()
        isGooglePayReadyFlow = MutableLiveData()
        isLinkEnabledFlow = MutableLiveData()
    }

    @Test
    fun `Only emits value if required flows have emitted values`() {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            initialSelection = initialSelectionFlow,
            currentSelection = currentSelectionFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
        )

        val state = mapper.createStateAndObserve()

        assertThat(state.value).isNull()

        paymentMethodsFlow.value = PaymentMethodFixtures.createCards(2)
        assertThat(state.value).isNull()

        initialSelectionFlow.value = SavedSelection.GooglePay
        assertThat(state.value).isNull()

        isGooglePayReadyFlow.value = true
        assertThat(state.value).isNull()

        isLinkEnabledFlow.value = true
        assertThat(state.value).isNotNull()
    }

    @Test
    fun `Doesn't include Google Pay and Link in payment flow`() {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            initialSelection = initialSelectionFlow,
            currentSelection = currentSelectionFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = false,
        )

        val state = mapper.createStateAndObserve()

        val cards = PaymentMethodFixtures.createCards(2)
        paymentMethodsFlow.value = cards
        initialSelectionFlow.value = SavedSelection.PaymentMethod(id = cards.first().id!!)
        isGooglePayReadyFlow.value = true
        isLinkEnabledFlow.value = true

        assertThat(state.value?.items).containsNoneOf(
            PaymentOptionsItem.GooglePay,
            PaymentOptionsItem.Link,
        )
    }

    @Test
    fun `Removing selected payment option results in saved selection being selected`() {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            initialSelection = initialSelectionFlow,
            currentSelection = currentSelectionFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
        )

        val state = mapper.createStateAndObserve()

        val cards = PaymentMethodFixtures.createCards(2)
        val selectedPaymentMethod = PaymentSelection.Saved(paymentMethod = cards.last())
        paymentMethodsFlow.value = cards
        initialSelectionFlow.value = SavedSelection.Link
        currentSelectionFlow.value = selectedPaymentMethod
        isGooglePayReadyFlow.value = true
        isLinkEnabledFlow.value = true

        assertThat(state.value).isNotNull()
        assertThat(state.value?.selectedItem).isEqualTo(
            PaymentOptionsItem.SavedPaymentMethod(selectedPaymentMethod.paymentMethod)
        )

        // Remove the currently selected payment option
        paymentMethodsFlow.value = cards - selectedPaymentMethod.paymentMethod
        currentSelectionFlow.value = null

        assertThat(state.value?.selectedItem).isEqualTo(PaymentOptionsItem.Link)
    }

    @Test
    fun `Removing selected payment option results in first available option if no saved selection`() {
        val mapper = PaymentOptionsStateMapper(
            paymentMethods = paymentMethodsFlow,
            initialSelection = initialSelectionFlow,
            currentSelection = currentSelectionFlow,
            isGooglePayReady = isGooglePayReadyFlow,
            isLinkEnabled = isLinkEnabledFlow,
            isNotPaymentFlow = true,
        )

        val state = mapper.createStateAndObserve()

        val cards = PaymentMethodFixtures.createCards(2)
        val selectedPaymentMethod = PaymentSelection.Saved(paymentMethod = cards.last())
        paymentMethodsFlow.value = cards
        initialSelectionFlow.value = SavedSelection.None
        currentSelectionFlow.value = selectedPaymentMethod
        isGooglePayReadyFlow.value = true
        isLinkEnabledFlow.value = true

        assertThat(state.value).isNotNull()
        assertThat(state.value?.selectedItem).isEqualTo(
            PaymentOptionsItem.SavedPaymentMethod(selectedPaymentMethod.paymentMethod)
        )

        // Remove the currently selected payment option
        paymentMethodsFlow.value = cards - selectedPaymentMethod.paymentMethod
        currentSelectionFlow.value = null

        assertThat(state.value?.selectedItem).isEqualTo(PaymentOptionsItem.GooglePay)
    }
}

private fun PaymentOptionsStateMapper.createStateAndObserve(): LiveData<PaymentOptionsState> {
    return this().also {
        it.observeForever {
            // Nothing to do here
        }
    }
}
