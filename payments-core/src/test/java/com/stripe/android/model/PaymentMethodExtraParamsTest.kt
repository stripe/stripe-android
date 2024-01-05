package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PaymentMethodExtraParamsTest {
    @Test
    fun createFromBacs() {
        assertThat(
            PaymentMethodExtraParams.BacsDebit(
                confirmed = true
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "bacs_debit" to mapOf(
                    "confirmed" to "true"
                )
            )
        )
    }
}
