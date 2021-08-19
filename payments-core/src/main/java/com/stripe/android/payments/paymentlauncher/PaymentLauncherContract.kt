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

    sealed class Args(
        open val publishableKey: String,
        open val stripeAccountId: String? = null
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        data class IntentConfirmationArgs(
            override val publishableKey: String,
            override val stripeAccountId: String? = null,
            val confirmStripeIntentParams: ConfirmStripeIntentParams
        ) : Args(publishableKey, stripeAccountId)

        @Parcelize
        data class PaymentIntentNextActionArgs(
            override val publishableKey: String,
            override val stripeAccountId: String? = null,
            val paymentIntentClientSecret: String
        ) : Args(publishableKey, stripeAccountId)

        @Parcelize
        data class SetupIntentNextActionArgs(
            override val publishableKey: String,
            override val stripeAccountId: String? = null,
            val setupIntentClientSecret: String
        ) : Args(publishableKey, stripeAccountId)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
