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

    @Test
    fun createFromCardWithValidatedScan() {
        assertThat(PaymentMethodExtraParams.Card(fromValidatedScan = true).toParamMap()).isEqualTo(
            mapOf(
                "card" to mapOf(
                    "validated_scan" to "true"
                )
            )
        )
    }

    @Test
    fun createFromCardWithoutValidatedScan() {
        assertThat(PaymentMethodExtraParams.Card(fromValidatedScan = false).toParamMap()).isEqualTo(
            mapOf(
                "card" to mapOf(
                    "validated_scan" to "false"
                )
            )
        )
    }
}
