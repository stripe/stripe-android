package com.stripe.android.payments.paymentlauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.model.ConfirmStripeIntentParams
import com.stripe.android.payments.core.injection.InjectorKey
import kotlinx.parcelize.Parcelize

/**
 * [ActivityResultContract] to start [PaymentLauncherConfirmationActivity] and return a [PaymentResult].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherContract :
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
        @InjectorKey open val injectorKey: Int,
        open val publishableKey: String,
        open val stripeAccountId: String?,
        open val enableLogging: Boolean,
        open val productUsage: Set<String>
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        data class IntentConfirmationArgs(
            override val injectorKey: Int,
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            val confirmStripeIntentParams: ConfirmStripeIntentParams
        ) : Args(injectorKey, publishableKey, stripeAccountId, enableLogging, productUsage)

        @Parcelize
        data class PaymentIntentNextActionArgs(
            override val injectorKey: Int,
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            val paymentIntentClientSecret: String
        ) : Args(injectorKey, publishableKey, stripeAccountId, enableLogging, productUsage)

        @Parcelize
        data class SetupIntentNextActionArgs(
            override val injectorKey: Int,
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            val setupIntentClientSecret: String
        ) : Args(injectorKey, publishableKey, stripeAccountId, enableLogging, productUsage)

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
