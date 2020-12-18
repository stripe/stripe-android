package com.stripe.android.paymentsheet

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import com.stripe.android.view.ActivityStarter
import kotlinx.parcelize.Parcelize

internal class PaymentSheetContract : ActivityResultContract<PaymentSheetContract.Args, PaymentResult>() {
    override fun createIntent(
        context: Context,
        input: Args
    ): Intent {
        return Intent(context, PaymentSheetActivity::class.java)
            .putExtra(EXTRA_ARGS, input)
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?
    ): PaymentResult {
        return PaymentSheet.Result.fromIntent(intent)?.status ?: PaymentResult.Failed(
            IllegalArgumentException("Failed to retrieve a PaymentResult."),
            null
        )
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        val config: PaymentSheet.Configuration?
    ) : ActivityStarter.Args {
        val googlePayConfig: PaymentSheet.GooglePayConfiguration? get() = config?.googlePay
        val isGooglePayEnabled: Boolean get() = googlePayConfig != null

        internal companion object {
            internal fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(ActivityStarter.Args.EXTRA)
            }
        }
    }

    companion object {
        internal const val EXTRA_ARGS: String = "extra_activity_args"
    }
}
