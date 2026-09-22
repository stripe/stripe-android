package com.stripe.android.financialconnections.model

import com.google.common.truth.Truth.assertThat
import com.stripe.android.financialconnections.model.FinancialConnectionsSessionManifest.Pane
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Test

internal class FinancialConnectionsSessionManifestTest {

    @Test
    fun `known pane decodes normally`() {
        assertThat(decodePane("consent")).isEqualTo(Pane.CONSENT)
    }

    @Test
    fun `unknown pane decodes to unknown`() {
        assertThat(decodePane("future_pane")).isEqualTo(Pane.UNKNOWN)
    }

    @Test
    fun `unexpected error pane decodes normally`() {
        assertThat(decodePane("unexpected_error")).isEqualTo(Pane.UNEXPECTED_ERROR)
    }

    private fun decodePane(value: String): Pane = Json.decodeFromJsonElement(JsonPrimitive(value))
}
