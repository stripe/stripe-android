package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ViewStateTest {
    @Test
    fun `FinishProcessing equals() with FinishProcessing param should return true`() {
        assertThat(ViewState.PaymentSheet.FinishProcessing {})
            .isEqualTo(ViewState.PaymentSheet.FinishProcessing {})
    }

    @Test
    fun `FinishProcessing equals() with not FinishProcessing param should return false`() {
        assertThat(ViewState.PaymentSheet.FinishProcessing {})
            .isNotEqualTo(ViewState.PaymentSheet.StartProcessing)
    }

    @Test
    fun `FinishProcessing hashCode() should return different values`() {
        val viewState1 = ViewState.PaymentSheet.FinishProcessing {}
        val viewState2 = ViewState.PaymentSheet.FinishProcessing {}

        assertThat(viewState1.hashCode())
            .isNotEqualTo(viewState2.hashCode())
    }
}
