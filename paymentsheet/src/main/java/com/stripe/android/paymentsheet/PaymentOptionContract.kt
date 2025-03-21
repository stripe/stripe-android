package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.paymentsheet.state.PaymentSheetState
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentOptionContract :
    ActivityResultContract<PaymentOptionContract.Args, PaymentOptionResult?>() {
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
        val state: PaymentSheetState.Full,
        val configuration: PaymentSheet.Configuration,
        val enableLogging: Boolean,
        val productUsage: Set<String>,
        val paymentElementCallbackIdentifier: String,
    ) : ActivityStarter.Args {
        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }

    internal companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}
