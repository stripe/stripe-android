package com.stripe.android.paymentelement

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.payment.EmbeddedPaymentElement

internal fun assertCompleted(result: EmbeddedPaymentElement.Result) {
    assertThat(result).isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
}
