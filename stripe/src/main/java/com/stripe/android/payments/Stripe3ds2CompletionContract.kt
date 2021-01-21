package com.stripe.android.payments

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.StripeIntentResult
import com.stripe.android.stripe3ds2.transaction.ChallengeCompletionIntentStarter
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
import com.stripe.android.view.ActivityStarter
import com.stripe.android.view.Stripe3ds2CompletionActivity

internal class Stripe3ds2CompletionContract :
    ActivityResultContract<Intent, PaymentFlowResult.Unvalidated>() {
    override fun createIntent(
        context: Context,
        input: Intent?
    ): Intent {
        return Intent(context, Stripe3ds2CompletionActivity::class.java)
            .putExtra(ActivityStarter.Args.EXTRA, input)
            .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentFlowResult.Unvalidated {
        return intent?.let {
            parsePaymentFlowResult(it)
        } ?: PaymentFlowResult.Unvalidated()
    }

    fun parsePaymentFlowResult(
        intent: Intent
    ): PaymentFlowResult.Unvalidated {
        return PaymentFlowResult.Unvalidated(
            clientSecret = intent.getStringExtra(EXTRA_CLIENT_SECRET),
            flowOutcome = parseFlowOutcome(intent),
            stripeAccountId = intent.getStringExtra(EXTRA_STRIPE_ACCOUNT)
        )
    }

    private fun parseFlowOutcome(intent: Intent): Int {
        val outcomeOrdinal = intent.getIntExtra(
            ChallengeCompletionIntentStarter.EXTRA_OUTCOME,
            UNKNOWN_FLOW_OUTCOME
        )

        return if (outcomeOrdinal == UNKNOWN_FLOW_OUTCOME) {
            StripeIntentResult.Outcome.UNKNOWN
        } else {
            when (ChallengeFlowOutcome.values()[outcomeOrdinal]) {
                ChallengeFlowOutcome.CompleteSuccessful ->
                    StripeIntentResult.Outcome.SUCCEEDED
                ChallengeFlowOutcome.Cancel ->
                    StripeIntentResult.Outcome.CANCELED
                ChallengeFlowOutcome.Timeout ->
                    StripeIntentResult.Outcome.TIMEDOUT
                ChallengeFlowOutcome.CompleteUnsuccessful,
                ChallengeFlowOutcome.ProtocolError,
                ChallengeFlowOutcome.RuntimeError ->
                    StripeIntentResult.Outcome.FAILED
            }
        }
    }

    internal companion object {
        const val EXTRA_CLIENT_SECRET = "extra_client_secret"
        const val EXTRA_STRIPE_ACCOUNT = "extra_stripe_account"
        private const val UNKNOWN_FLOW_OUTCOME = -1
    }
}
