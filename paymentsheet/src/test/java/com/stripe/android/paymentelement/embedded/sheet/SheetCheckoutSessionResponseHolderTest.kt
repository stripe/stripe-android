package com.stripe.android.paymentelement.embedded.sheet

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponse
import com.stripe.android.paymentsheet.repositories.CheckoutSessionResponseFactory
import org.junit.Test

internal class SheetCheckoutSessionResponseHolderTest {
    @Test
    fun `response is persisted and restored from SavedStateHandle`() {
        val savedStateHandle = SavedStateHandle()
        val holder = SheetCheckoutSessionResponseHolder(savedStateHandle)
        val response = CheckoutSessionResponseFactory.create()

        assertThat(holder.response).isNull()
        holder.set(response)

        assertThat(
            savedStateHandle.get<CheckoutSessionResponse>(
                SheetCheckoutSessionResponseHolder.CHECKOUT_SESSION_RESPONSE_KEY,
            ),
        )
            .isEqualTo(response)
        assertThat(SheetCheckoutSessionResponseHolder(savedStateHandle).response)
            .isEqualTo(response)
    }

    @Test
    fun `setting null clears response`() {
        val holder = SheetCheckoutSessionResponseHolder(SavedStateHandle())
        holder.set(CheckoutSessionResponseFactory.create())

        holder.set(null)

        assertThat(holder.response).isNull()
    }
}
