package com.stripe.android.paymentsheet.repositories

import com.google.common.truth.Truth.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CheckoutClientPollResponseJsonParserTest {

    @Test
    fun `parses poll response`() {
        val response = CheckoutClientPollResponseJsonParser.parse(pollJson())

        assertThat(response).isEqualTo(
            CheckoutClientPollResponse(
                id = "cs_test_123",
                confirmationStatus = CheckoutClientPollResponse.ConfirmationStatus.PENDING,
                status = CheckoutSessionResponse.Status.COMPLETE,
                paymentStatus = CheckoutSessionResponse.PaymentStatus.PAID,
            )
        )
    }

    @Test
    fun `parses every confirmation status`() {
        val statuses = mapOf(
            "not_started" to CheckoutClientPollResponse.ConfirmationStatus.NOT_STARTED,
            "pending" to CheckoutClientPollResponse.ConfirmationStatus.PENDING,
            "succeeded" to CheckoutClientPollResponse.ConfirmationStatus.SUCCEEDED,
            "failed" to CheckoutClientPollResponse.ConfirmationStatus.FAILED,
        )

        statuses.forEach { (value, expected) ->
            val response = CheckoutClientPollResponseJsonParser.parse(
                pollJson().put("confirmation_status", value)
            )
            assertThat(response?.confirmationStatus).isEqualTo(expected)
        }
    }

    @Test
    fun `rejects unknown confirmation status`() {
        val response = CheckoutClientPollResponseJsonParser.parse(
            pollJson().put("confirmation_status", "processing")
        )

        assertThat(response).isNull()
    }

    @Test
    fun `rejects unknown payment status`() {
        val response = CheckoutClientPollResponseJsonParser.parse(
            pollJson().put("payment_status", "PAID")
        )

        assertThat(response).isNull()
    }

    @Test
    fun `rejects missing session status`() {
        val json = pollJson().apply { remove("status") }

        assertThat(CheckoutClientPollResponseJsonParser.parse(json)).isNull()
    }

    private fun pollJson() = JSONObject(
        """
        {
          "id": "cs_test_123",
          "confirmation_status": "pending",
          "status": "complete",
          "payment_status": "paid"
        }
        """.trimIndent()
    )
}
