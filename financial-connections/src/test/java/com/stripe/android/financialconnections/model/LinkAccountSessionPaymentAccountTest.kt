package com.stripe.android.financialconnections.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import org.junit.Test

internal class LinkAccountSessionPaymentAccountTest {

    @Test
    fun `decodes generated payment detail IDs`() {
        val response = Json.decodeFromString(
            LinkAccountSessionPaymentAccount.serializer(),
            """{"id":"la_123","generated_payment_detail_ids":["csmrpd_1","csmrpd_2"]}""",
        )

        assertThat(response.generatedPaymentDetailIds).containsExactly("csmrpd_1", "csmrpd_2").inOrder()
    }

    @Test
    fun `missing generated payment detail IDs decodes as empty`() {
        val response = Json.decodeFromString(
            LinkAccountSessionPaymentAccount.serializer(),
            """{"id":"la_123"}""",
        )

        assertThat(response.generatedPaymentDetailIds).isEmpty()
    }
}
