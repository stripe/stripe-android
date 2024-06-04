package com.stripe.android.paymentsheet.paymentdatacollection.polling

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import com.stripe.android.payments.PaymentFlowResult
import kotlinx.parcelize.Parcelize

private const val EXTRA_ARGS = "extra_args"

internal class PollingContract :
    ActivityResultContract<PollingContract.Args, PaymentFlowResult.Unvalidated>() {

    override fun createIntent(context: Context, input: Args): Intent {
        val args = bundleOf(EXTRA_ARGS to input)
        return Intent(context, PollingActivity::class.java).putExtras(args)
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentFlowResult.Unvalidated {
        return PaymentFlowResult.Unvalidated.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val clientSecret: String,
        @ColorInt val statusBarColor: Int?,
        val timeLimitInSeconds: Int,
        val initialDelayInSeconds: Int,
        val maxAttempts: Int,
        @StringRes val ctaText: Int,
    ) : Parcelable {

        internal companion object {
            fun fromIntent(intent: Intent): Args? {
                @Suppress("DEPRECATION")
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
