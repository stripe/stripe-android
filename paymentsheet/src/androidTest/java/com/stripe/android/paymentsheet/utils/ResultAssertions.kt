package com.stripe.android.paymentsheet.utils

import com.google.common.truth.Truth.assertThat
import com.stripe.android.paymentsheet.PaymentSheetResult

internal fun assertCompleted(result: PaymentSheetResult) {
    assertThat(result).isInstanceOf(PaymentSheetResult.Completed::class.java)
}

internal fun assertFailed(result: PaymentSheetResult) {
    assertThat(result).isInstanceOf(PaymentSheetResult.Failed::class.java)
}

@Suppress("UNUSED_PARAMETER")
internal fun expectNoResult(result: PaymentSheetResult) {
    error("Shouldn't call PaymentSheetResultCallback")
}
