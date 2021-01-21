package com.stripe.android.payments

import android.app.Activity
import android.content.Intent
import androidx.core.os.bundleOf
import com.google.common.truth.Truth.assertThat
import com.stripe.android.StripeIntentResult
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
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
                        bundleOf(
                            "extra_client_secret" to "client_secret_123",
                            "extra_outcome" to ChallengeFlowOutcome.Timeout.ordinal,
                            "extra_stripe_account" to "acct_123"
                        )
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
                        bundleOf(
                            "extra_client_secret" to "client_secret_123",
                            "extra_outcome" to 5000,
                            "extra_stripe_account" to "acct_123"
                        )
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
