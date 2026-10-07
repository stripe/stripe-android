package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
internal class CheckoutSessionPollResponseJsonParserTest {
    @Test
    fun `parses supported states`(@TestParameter state: CheckoutSessionPollResponse.State) {
        val result = CheckoutSessionPollResponseJsonParser.parse(base().put("state", state.code))
        assertThat(result?.state).isEqualTo(state)
        assertThat(result?.sessionId).isEqualTo("cs_test")
        assertThat(result?.paymentObjectStatus).isNull()
    }

    @Test
    fun `parses payment object status`() {
        val result = CheckoutSessionPollResponseJsonParser.parse(
            base().put("payment_object_status", "requires_payment_method")
        )
        assertThat(result?.paymentObjectStatus).isEqualTo("requires_payment_method")
    }

    @Test
    fun `accepts other payment object statuses`() {
        val result = CheckoutSessionPollResponseJsonParser.parse(base().put("payment_object_status", "future_status"))
        assertThat(result?.paymentObjectStatus).isEqualTo("future_status")
    }

    @Test
    fun `rejects missing required fields`(@TestParameter(value = ["state", "session_id"]) field: String) {
        val json = base().apply { remove(field) }
        assertThat(CheckoutSessionPollResponseJsonParser.parse(json)).isNull()
    }

    @Test
    fun `rejects invalid state values`(@TestParameter(value = ["unknown", ""]) state: String) {
        assertThat(CheckoutSessionPollResponseJsonParser.parse(base().put("state", state))).isNull()
    }

    @Test
    fun `rejects a non string payment object status`() {
        assertThat(CheckoutSessionPollResponseJsonParser.parse(base().put("payment_object_status", 123))).isNull()
    }

    @Test
    fun `rejects a null state`() {
        assertThat(CheckoutSessionPollResponseJsonParser.parse(base().put("state", JSONObject.NULL))).isNull()
    }

    private fun base() = JSONObject().put("session_id", "cs_test").put("state", "active")
}
