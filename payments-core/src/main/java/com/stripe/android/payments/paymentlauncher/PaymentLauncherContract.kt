package com.stripe.android.payments.paymentlauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.core.injection.InjectorKey
import com.stripe.android.model.ConfirmStripeIntentParams
import kotlinx.parcelize.Parcelize

/**
 * [ActivityResultContract] to start [PaymentLauncherConfirmationActivity] and return a [PaymentResult].
 */
// TODO: Why is this publicly accessible?
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherContract :
    ActivityResultContract<PaymentLauncherContract.Args, PaymentResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        input.statusBarColor = (context as? Activity)?.window?.statusBarColor
        return Intent(
            context,
            PaymentLauncherConfirmationActivity::class.java
        ).putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): PaymentResult {
        return PaymentResult.fromIntent(intent)
    }

    @Deprecated(
        message = "This isn't meant for public usage and will be removed in a future release.",
    )
    sealed class Args(
        @InjectorKey open val injectorKey: String,
        open val publishableKey: String,
        open val stripeAccountId: String?,
        open val enableLogging: Boolean,
        open val productUsage: Set<String>,
        @ColorInt open var statusBarColor: Int? = null
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release.",
        )
        @Parcelize
        data class IntentConfirmationArgs internal constructor(
            @InjectorKey override val injectorKey: String,
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            val confirmStripeIntentParams: ConfirmStripeIntentParams,
            @ColorInt override var statusBarColor: Int? = null
        ) : Args(
            injectorKey,
            publishableKey,
            stripeAccountId,
            enableLogging,
            productUsage,
            statusBarColor
        )

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release.",
        )
        @Parcelize
        data class PaymentIntentNextActionArgs internal constructor(
            @InjectorKey override val injectorKey: String,
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            val paymentIntentClientSecret: String,
            @ColorInt override var statusBarColor: Int? = null
        ) : Args(
            injectorKey,
            publishableKey,
            stripeAccountId,
            enableLogging,
            productUsage,
            statusBarColor
        )

        @Deprecated(
            message = "This isn't meant for public usage and will be removed in a future release.",
        )
        @Parcelize
        data class SetupIntentNextActionArgs internal constructor(
            @InjectorKey override val injectorKey: String,
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            val setupIntentClientSecret: String,
            @ColorInt override var statusBarColor: Int? = null
        ) : Args(
            injectorKey,
            publishableKey,
            stripeAccountId,
            enableLogging,
            productUsage,
            statusBarColor
        )

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
