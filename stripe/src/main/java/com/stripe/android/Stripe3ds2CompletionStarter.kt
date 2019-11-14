package com.stripe.android

import android.os.Bundle
import androidx.annotation.IntDef
import com.stripe.android.model.StripeIntent
import com.stripe.android.view.AuthActivityStarter
import com.stripe.android.view.PaymentRelayActivity
import com.stripe.android.view.StripeIntentResultExtras

internal class Stripe3ds2CompletionStarter(
    private val host: AuthActivityStarter.Host,
    private val requestCode: Int
) : AuthActivityStarter<Stripe3ds2CompletionStarter.Args> {

    override fun start(args: Args) {
        val extras = Bundle()
        extras.putString(StripeIntentResultExtras.CLIENT_SECRET,
            args.stripeIntent.clientSecret)
        extras.putInt(StripeIntentResultExtras.FLOW_OUTCOME, args.outcome)
        host.startActivityForResult(PaymentRelayActivity::class.java, extras, requestCode)
    }

    @IntDef(ChallengeFlowOutcome.COMPLETE_SUCCESSFUL, ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL,
        ChallengeFlowOutcome.CANCEL, ChallengeFlowOutcome.TIMEOUT,
        ChallengeFlowOutcome.PROTOCOL_ERROR, ChallengeFlowOutcome.RUNTIME_ERROR)
    @Retention(AnnotationRetention.SOURCE)
    internal annotation class ChallengeFlowOutcome {
        companion object {
            internal const val COMPLETE_SUCCESSFUL = 0
            internal const val COMPLETE_UNSUCCESSFUL = 1
            internal const val CANCEL = 2
            internal const val TIMEOUT = 3
            internal const val PROTOCOL_ERROR = 4
            internal const val RUNTIME_ERROR = 5
        }
    }

    internal data class Args internal constructor(
        val stripeIntent: StripeIntent,
        @param:ChallengeFlowOutcome @field:ChallengeFlowOutcome
        private val challengeFlowOutcome: Int
    ) {
        internal val outcome: Int
            @StripeIntentResult.Outcome
            @JvmSynthetic
            get() = when (challengeFlowOutcome) {
                ChallengeFlowOutcome.COMPLETE_SUCCESSFUL -> StripeIntentResult.Outcome.SUCCEEDED
                ChallengeFlowOutcome.COMPLETE_UNSUCCESSFUL -> StripeIntentResult.Outcome.FAILED
                ChallengeFlowOutcome.CANCEL -> StripeIntentResult.Outcome.CANCELED
                ChallengeFlowOutcome.TIMEOUT -> StripeIntentResult.Outcome.TIMEDOUT
                else -> StripeIntentResult.Outcome.FAILED
            }
    }
}
