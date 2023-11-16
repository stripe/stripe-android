package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class AccountParamsTest {

    @Test
    fun `Creates correct params when provided with company business data`() {
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
    fun `Creates correct params when provided with no business data`() {
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

    @Test
    fun `Creates correct params when provided with individual and company business data`() {
        val params = AccountParams.create(
            tosShownAndAccepted = true,
            individual = AccountParams.BusinessTypeParams.Individual(firstName = "Patrick", lastName = "C"),
            company = AccountParams.BusinessTypeParams.Company(name = "Stripe"),
        )

        val expected = mapOf(
            "account" to mapOf(
                "tos_shown_and_accepted" to true,
                "business_type" to "individual",
                "individual" to mapOf(
                    "first_name" to "Patrick",
                    "last_name" to "C",
                ),
                "company" to mapOf(
                    "name" to "Stripe",
                ),
            )
        )

        assertThat(params.toParamMap()).isEqualTo(expected)
    }
}
