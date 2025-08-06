package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.elements.payment.FlowController
import com.stripe.android.elements.payment.PaymentSheet

internal fun assertCompleted(result: PaymentSheet.Result) {
    assertThat(result).isInstanceOf(PaymentSheet.Result.Completed::class.java)
}

internal fun assertFailed(result: PaymentSheet.Result) {
    assertThat(result).isInstanceOf(PaymentSheet.Result.Failed::class.java)
}

@Suppress("UNUSED_PARAMETER")
internal fun expectNoResult(result: PaymentSheet.Result) {
    error("Shouldn't call PaymentSheet.ResultCallback")
}

internal fun assertCompleted(result: FlowController.Result) {
    assertThat(result).isInstanceOf(FlowController.Result.Completed::class.java)
}

internal fun assertFailed(result: FlowController.Result) {
    assertThat(result).isInstanceOf(FlowController.Result.Failed::class.java)
}
