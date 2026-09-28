package com.stripe.android.common.taptoadd.ui

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test

internal class DefaultTapToAddStateHolderTest {
    @Test
    fun `state returns null when handle has no state`() {
        val savedStateHandle = SavedStateHandle()
        val holder = DefaultTapToAddStateHolder(savedStateHandle)

        assertThat(holder.state).isNull()
    }

    @Test
    fun `state returns value from handle after setState is called with CardAdded`() {
        val savedStateHandle = SavedStateHandle()
        val holder = DefaultTapToAddStateHolder(savedStateHandle)
        val paymentMethod = PaymentMethodFactory.card(random = true)

        holder.setState(TapToAddStateHolder.State.CardAdded(paymentMethod))

        assertThat(holder.state).isEqualTo(TapToAddStateHolder.State.CardAdded(paymentMethod))
    }

    @Test
    fun `state returns value from handle after setState is called with Confirmation`() {
        val savedStateHandle = SavedStateHandle()
        val holder = DefaultTapToAddStateHolder(savedStateHandle)
        val paymentMethod = PaymentMethodFactory.card(random = true)

        holder.setState(TapToAddStateHolder.State.Confirmation(paymentMethod, linkInput = null))

        assertThat(holder.state).isEqualTo(
            TapToAddStateHolder.State.Confirmation(paymentMethod, linkInput = null)
        )
    }

    @Test
    fun `state returns value in handle when initialized with CardAdded`() {
        val paymentMethod = PaymentMethodFactory.card(random = true)
        val initialState = TapToAddStateHolder.State.CardAdded(paymentMethod)
        val savedStateHandle = SavedStateHandle().apply {
            set(TAP_TO_ADD_STATE_KEY, initialState)
        }
        val holder = DefaultTapToAddStateHolder(savedStateHandle)

        assertThat(holder.state).isEqualTo(initialState)
    }

    @Test
    fun `setState with null clears state`() {
        val paymentMethod = PaymentMethodFactory.card(random = true)
        val savedStateHandle = SavedStateHandle()
        val holder = DefaultTapToAddStateHolder(savedStateHandle)

        holder.setState(TapToAddStateHolder.State.CardAdded(paymentMethod))
        assertThat(holder.state).isNotNull()

        holder.setState(null)
        assertThat(holder.state).isNull()
    }

    private companion object {
        const val TAP_TO_ADD_STATE_KEY = "TAP_TO_ADD_STATE"
    }
}
