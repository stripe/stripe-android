package com.stripe.android.common.taptoadd.ui

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentMethod
import com.stripe.android.testing.PaymentMethodFactory
import org.junit.Test

internal class DefaultTapToAddPaymentMethodHolderTest {
    @Test
    fun `paymentMethod returns null when handle has no payment method`() {
        val savedStateHandle = SavedStateHandle()
        val holder = DefaultTapToAddPaymentMethodHolder(savedStateHandle)

        assertThat(holder.paymentMethod).isNull()
        assertThat(savedStateHandle.getPaymentMethod()).isNull()
    }

    @Test
    fun `paymentMethod returns value from handle after setPaymentMethod is called`() {
        val savedStateHandle = SavedStateHandle()
        val holder = DefaultTapToAddPaymentMethodHolder(savedStateHandle)
        val paymentMethod = PaymentMethodFactory.card(random = true)

        holder.setPaymentMethod(paymentMethod)

        assertThat(holder.paymentMethod).isEqualTo(paymentMethod)
        assertThat(savedStateHandle.getPaymentMethod()).isEqualTo(paymentMethod)
    }

    @Test
    fun `paymentMethod returns value in handle when initialized`() {
        val paymentMethod = PaymentMethodFactory.card(random = true)
        val savedStateHandle = SavedStateHandle().apply { setPaymentMethod(paymentMethod) }
        val holder = DefaultTapToAddPaymentMethodHolder(savedStateHandle)

        assertThat(holder.paymentMethod).isEqualTo(paymentMethod)
        assertThat(savedStateHandle.getPaymentMethod()).isEqualTo(paymentMethod)
    }

    private fun SavedStateHandle.getPaymentMethod(): PaymentMethod? {
        return get(EXPECTED_HANDLE_KEY)
    }

    private fun SavedStateHandle.setPaymentMethod(
        paymentMethod: PaymentMethod,
    ) {
        set(EXPECTED_HANDLE_KEY, paymentMethod)
    }

    private companion object {
        const val EXPECTED_HANDLE_KEY = "STRIPE_TAP_TO_ADD_PAYMENT_METHOD"
    }
}
