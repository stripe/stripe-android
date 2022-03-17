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
    fun blikToParamMap_withCode_includeCode() {
        val blikCode = "123456"
        assertThat(
            PaymentMethodOptionsParams.Blik(
                code = blikCode
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                PaymentMethod.Type.Blik.code to mapOf(
                    PaymentMethodOptionsParams.Blik.PARAM_CODE to blikCode
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

    @Test
    fun usBankAccountToParamMap_withNoData_shouldHaveEmptyParams() {
        assertThat(
            PaymentMethodOptionsParams.USBankAccount()
                .toParamMap()
        ).isEmpty()
    }
}
