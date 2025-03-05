@file:OptIn(ExperimentalEmbeddedPaymentElementApi::class)

package com.stripe.android.paymentelement

import com.google.common.truth.Truth.assertThat

internal fun assertCompleted(result: EmbeddedPaymentElement.Result) {
    assertThat(result).isInstanceOf(EmbeddedPaymentElement.Result.Completed::class.java)
}
