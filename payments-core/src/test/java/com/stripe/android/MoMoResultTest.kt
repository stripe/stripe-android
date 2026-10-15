package com.stripe.android

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.stripe.android.model.PaymentIntentFixtures
import com.stripe.android.model.StripeIntent
import com.stripe.android.model.parsers.PaymentMethodJsonParser
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class MoMoResultTest {
    @Test
    fun `processing after redirect is a successful result`() = runScenario(
        status = StripeIntent.Status.Processing,
        outcomeFromFlow = StripeIntentResult.Outcome.UNKNOWN,
        nextActionData = null,
    ) { result ->
        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.SUCCEEDED)
    }

    @Test
    fun `explicit cancellation is preserved while processing`() = runScenario(
        status = StripeIntent.Status.Processing,
        outcomeFromFlow = StripeIntentResult.Outcome.CANCELED,
        nextActionData = null,
    ) { result ->
        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    @Test
    fun `unfinished redirect is canceled`() = runScenario(
        status = StripeIntent.Status.RequiresAction,
        outcomeFromFlow = StripeIntentResult.Outcome.UNKNOWN,
        nextActionData = StripeIntent.NextActionData.RedirectToUrl(
            url = Uri.parse("https://example.com/authorize"),
            returnUrl = "stripesdk://return",
        ),
    ) { result ->
        assertThat(result.outcome).isEqualTo(StripeIntentResult.Outcome.CANCELED)
    }

    private fun runScenario(
        status: StripeIntent.Status,
        outcomeFromFlow: Int,
        nextActionData: StripeIntent.NextActionData?,
        block: (PaymentIntentResult) -> Unit,
    ) {
        val paymentMethod = PaymentMethodJsonParser().parse(JSONObject("""{"id":"pm_123","type":"momo"}"""))
        val intent = PaymentIntentFixtures.PI_REQUIRES_PAYMENT_METHOD.copy(
            paymentMethodTypes = listOf("momo"),
            paymentMethod = paymentMethod,
            status = status,
            nextActionData = nextActionData,
        )

        block(
            PaymentIntentResult(
                intent = intent,
                outcomeFromFlow = outcomeFromFlow,
                failureMessage = null,
            )
        )
    }
}
