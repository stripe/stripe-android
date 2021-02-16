package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.model.PaymentIntent
import com.stripe.android.model.PaymentMethod
import com.stripe.android.paymentsheet.analytics.SessionId
import com.stripe.android.paymentsheet.model.PaymentSelection
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentOptionContract : ActivityResultContract<PaymentOptionContract.Args, PaymentOptionResult?>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, PaymentOptionsActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentOptionResult? {
        return PaymentOptionResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val paymentIntent: PaymentIntent,
        val paymentMethods: List<PaymentMethod>,
        val sessionId: SessionId,
        val config: PaymentSheet.Configuration?,
        val isGooglePayReady: Boolean,
        val newCard: PaymentSelection.New.Card?
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}
