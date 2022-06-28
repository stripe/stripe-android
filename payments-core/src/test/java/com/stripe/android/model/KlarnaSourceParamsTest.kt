package com.stripe.android.model

import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class KlarnaSourceParamsTest {

    @Test
    fun `date and month should be padded when single digit`() {
        assertThat(
            KlarnaSourceParams(
                purchaseCountry = "UK",
                lineItems = emptyList(),
                customPaymentMethods = emptySet(),
                billingDob = DateOfBirth(1, 1, 1990)
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "product" to "payment",
                "purchase_country" to "UK",
                "owner_dob_day" to "01",
                "owner_dob_month" to "01",
                "owner_dob_year" to "1990"
            )
        )
    }

    @Test
    fun `date and month should not be padded when two digit`() {
        assertThat(
            KlarnaSourceParams(
                purchaseCountry = "UK",
                lineItems = emptyList(),
                customPaymentMethods = emptySet(),
                billingDob = DateOfBirth(11, 12, 1990)
            ).toParamMap()
        ).isEqualTo(
            mapOf(
                "product" to "payment",
                "purchase_country" to "UK",
                "owner_dob_day" to "11",
                "owner_dob_month" to "12",
                "owner_dob_year" to "1990"
            )
        )
    }
}
