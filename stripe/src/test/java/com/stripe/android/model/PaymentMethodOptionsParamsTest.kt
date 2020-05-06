package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class PaymentMethodOptionsParamsTest {

    @Test
    fun cardToParamMap_withNetwork_shouldOnlyIncludeNetwork() {
        assertThat(
            PaymentMethodOptionsParams.Card(
                network = "visa"
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "card" to mapOf(
                    "network" to "visa"
                )
            )
        )
    }

    @Test
    fun cardToParamMap_withNoData_shouldHaveEmptyParams() {
        assertThat(
            PaymentMethodOptionsParams.Card()
                .toParamMap()
        ).isEmpty()
    }
}
