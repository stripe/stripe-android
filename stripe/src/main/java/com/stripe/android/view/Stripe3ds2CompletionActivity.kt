package com.stripe.android.view

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.stripe.android.PaymentController
import com.stripe.android.StripeIntentResult
import com.stripe.android.stripe3ds2.transaction.ChallengeCompletionIntentStarter
import com.stripe.android.stripe3ds2.transaction.ChallengeFlowOutcome
import com.ults.listeners.SdkChallengeInterface.UL_HANDLE_CHALLENGE_ACTION

class Stripe3ds2CompletionActivity : AppCompatActivity() {

    private val flowOutcome: Int by lazy {
        val outcomeOrdinal = intent.getIntExtra(
            ChallengeCompletionIntentStarter.EXTRA_OUTCOME,
            UNKNOWN_FLOW_OUTCOME
        )

        if (outcomeOrdinal == UNKNOWN_FLOW_OUTCOME) {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = PaymentController.Result(
            clientSecret = intent.getStringExtra(EXTRA_CLIENT_SECRET),
            flowOutcome = flowOutcome
        )

        LocalBroadcastManager.getInstance(this)
            .sendBroadcast(Intent().setAction(UL_HANDLE_CHALLENGE_ACTION))
        setResult(
            Activity.RESULT_OK,
            Intent()
                .putExtras(result.toBundle())
        )
        finish()
    }

    internal companion object {
        const val EXTRA_CLIENT_SECRET = "extra_client_secret"
        private const val UNKNOWN_FLOW_OUTCOME = -1
    }
}
