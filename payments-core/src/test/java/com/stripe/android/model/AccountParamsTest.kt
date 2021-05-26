package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class AccountParamsTest {

    @Test
    fun toParamMap_withBusinessData() {
        val company = AccountParams.BusinessTypeParams.Company(name = "Stripe")

        assertThat(
            AccountParams.create(
                true,
                company
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "account" to mapOf(
                    "tos_shown_and_accepted" to true,
                    "business_type" to "company",
                    "company" to company.toParamMap()
                )
            )
        )
    }

    @Test
    fun toParamMap_withNoBusinessData() {
        assertThat(
            AccountParams.create(true).toParamMap()
        ).isEqualTo(
            mapOf(
                "account" to mapOf(
                    "tos_shown_and_accepted" to true
                )
            )
        )
    }
}
