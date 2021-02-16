package com.stripe.android.payments

import android.app.Activity
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class Stripe3ds2CompletionContractTest {

    @Test
    fun `parseResult() with populated extras should return valid PaymentFlowResult`() {
        assertThat(
            Stripe3ds2CompletionContract().parseResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtras(
                        PaymentFlowResult.Unvalidated(
                            clientSecret = "client_secret_123",
                            flowOutcome = StripeIntentResult.Outcome.TIMEDOUT,
                            stripeAccountId = "acct_123"
                        ).toBundle()
                    )
            ).validate()
        ).isEqualTo(
            PaymentFlowResult.Validated(
                clientSecret = "client_secret_123",
                flowOutcome = StripeIntentResult.Outcome.TIMEDOUT,
                stripeAccountId = "acct_123"
            )
        )
    }

    @Test
    fun `parseResult() with out of bounds ordinal should return valid PaymentFlowResult`() {
        assertThat(
            Stripe3ds2CompletionContract().parseResult(
                Activity.RESULT_OK,
                Intent()
                    .putExtras(
                        PaymentFlowResult.Unvalidated(
                            clientSecret = "client_secret_123",
                            flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                            stripeAccountId = "acct_123"
                        ).toBundle()
                    )
            ).validate()
        ).isEqualTo(
            PaymentFlowResult.Validated(
                clientSecret = "client_secret_123",
                flowOutcome = StripeIntentResult.Outcome.UNKNOWN,
                stripeAccountId = "acct_123"
            )
        )
    }

    @Test
    fun `parseResult() with empty Intent should return empty unvalidated PaymentFlowResult`() {
        assertThat(
            Stripe3ds2CompletionContract().parseResult(
                Activity.RESULT_OK,
                Intent()
            )
        ).isEqualTo(
            PaymentFlowResult.Unvalidated()
        )
    }
}
