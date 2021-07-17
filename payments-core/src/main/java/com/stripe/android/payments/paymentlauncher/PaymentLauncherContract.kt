package com.stripe.android.payments.paymentlauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.model.ConfirmStripeIntentParams
import kotlinx.parcelize.Parcelize

/**
 * [ActivityResultContract] to start [PaymentLauncherConfirmationActivity] and return a [PaymentResult].
 */
internal class PaymentLauncherContract :
    ActivityResultContract<PaymentLauncherContract.Args, PaymentResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(
            context,
            PaymentLauncherConfirmationActivity::class.java
        ).putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return PaymentResult.fromIntent(intent)
    }

    sealed class Args : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        data class IntentConfirmationArgs(
            val confirmStripeIntentParams: ConfirmStripeIntentParams
        ) : Args()

        @Parcelize
        data class PaymentIntentNextActionArgs(
            val paymentIntentClientSecret: String
        ) : Args()

        @Parcelize
        data class SetupIntentNextActionArgs(
            val setupIntentClientSecret: String
        ) : Args()

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
