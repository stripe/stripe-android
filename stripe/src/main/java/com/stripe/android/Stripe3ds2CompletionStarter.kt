package com.stripe.android

import android.os.Bundle
import android.support.annotation.IntDef
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import com.stripe.android.view.StripeIntentResultExtras

internal class Stripe3ds2CompletionStarter(
    private val host: AuthActivityStarter.Host,
    private val requestCode: Int
) : AuthActivityStarter<Stripe3ds2CompletionStarter.StartData> {

    override fun start(data: StartData) {
        val extras = Bundle()
        extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
            data.stripeIntent.clientSecret)
        extras.putInt(StripeIntentResultExtras.FLOW_OUTCOME, data.outcome)
        host.startActivityForResult(PaymentRelayActivity::class.java, extras, requestCode)
    }

    @IntDef(ChallengeFlowOutcome.COMPLETE_SUCCESSFUL, ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL,
        ChallengeFlowOutcome.CANCEL, ChallengeFlowOutcome.TIMEOUT,
        ChallengeFlowOutcome.PROTOCOL_ERROR, ChallengeFlowOutcome.RUNTIME_ERROR)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class ChallengeFlowOutcome {
        companion object {
            const val COMPLETE_SUCCESSFUL = 0
            const val COMPLETE_UNSUCCESSFUL = 1
            const val CANCEL = 2
            const val TIMEOUT = 3
            const val PROTOCOL_ERROR = 4
            const val RUNTIME_ERROR = 5
        }
    }

    internal data class StartData(
        val stripeIntent: StripeIntent,
        @param:ChallengeFlowOutcome @field:ChallengeFlowOutcome
        private val challengeFlowOutcome: Int
    ) {
        val outcome: Int
            @StripeIntentResult.Outcome
            get() = when (challengeFlowOutcome) {
                ChallengeFlowOutcome.COMPLETE_SUCCESSFUL -> StripeIntentResult.Outcome.SUCCEEDED
                ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL -> StripeIntentResult.Outcome.FAILED
                ChallengeFlowOutcome.CANCEL -> StripeIntentResult.Outcome.CANCELED
                ChallengeFlowOutcome.TIMEOUT -> StripeIntentResult.Outcome.TIMEDOUT
                else -> StripeIntentResult.Outcome.FAILED
            }
    }
}
