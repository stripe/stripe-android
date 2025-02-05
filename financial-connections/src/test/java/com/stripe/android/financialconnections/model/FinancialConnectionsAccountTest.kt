package com.stripe.android.financialconnections.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test

class FinancialConnectionsAccountTest {
    @Test
    fun `Serialized JSON object contains 'object' field`() {
        assertThat(
            Json.encodeToJsonElement(FinancialConnectionsAccountFixtures.SAVINGS_ACCOUNT)
                .jsonObject["object"]?.jsonPrimitive?.content
        )
            .isEqualTo(FinancialConnectionsAccount.OBJECT_NEW)
    }
}
