package com.stripe.android.payments.paymentlauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.os.bundleOf
import com.stripe.android.model.ConfirmStripeIntentParams
import kotlinx.parcelize.Parcelize

/**
 * [ActivityResultContract] to start [PaymentConfirmationActivity] and return a [PaymentResult].
 */
internal class PaymentLauncherHostContract :
    ActivityResultContract<PaymentLauncherHostContract.Args, PaymentResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(context, PaymentConfirmationActivity::class.java).putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return PaymentResult.fromIntent(intent)
    }

    @Parcelize
    internal data class Args(
        val confirmStripeIntentParams: ConfirmStripeIntentParams? = null,
        val paymentIntentClientSecret: String? = null,
        val setupIntentClientSecret: String? = null,
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        /**
         * Validate the Arguments, only one value can be set.
         */
        fun isValid(): Boolean {
            var nonNullParamCount = 0
            listOf(
                confirmStripeIntentParams,
                paymentIntentClientSecret,
                setupIntentClientSecret
            ).forEach {
                if (it != null) {
                    nonNullParamCount++
                }
            }
            return nonNullParamCount == 1
        }

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
