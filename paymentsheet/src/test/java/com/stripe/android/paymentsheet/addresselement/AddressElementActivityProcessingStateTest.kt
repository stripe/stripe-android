package com.stripe.android.paymentsheet.addresselement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

internal class AddressElementActivityProcessingStateTest {
    @Test
    fun `processing state blocks a duplicate save until the current save finishes`() {
        val processingState = AddressElementActivityProcessingState()

        assertThat(processingState.isProcessing.value).isFalse()
        assertThat(processingState.tryStartProcessing()).isTrue()
        assertThat(processingState.isProcessing.value).isTrue()
        assertThat(processingState.tryStartProcessing()).isFalse()

        processingState.finishProcessing()

        assertThat(processingState.isProcessing.value).isFalse()
        assertThat(processingState.tryStartProcessing()).isTrue()
    }
}
