package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class AddressElementActivityProcessingStateTest {
    @Test
    fun `processing blocks duplicate work until current work finishes`() {
        val state = AddressElementActivityProcessingState()

        assertThat(state.tryStartProcessing()).isTrue()
        assertThat(state.isProcessing.value).isTrue()
        assertThat(state.tryStartProcessing()).isFalse()

        state.finishProcessing()

        assertThat(state.isProcessing.value).isFalse()
        assertThat(state.tryStartProcessing()).isTrue()
    }
}
