package com.stripe.android.payments.paymentlauncher

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.model.ConfirmStripeIntentParams
import kotlinx.parcelize.Parcelize

/**
 * [ActivityResultContract] to start [PaymentLauncherConfirmationActivity] and return a [PaymentResult].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class PaymentLauncherContract :
    ActivityResultContract<PaymentLauncherContract.Args, InternalPaymentResult>() {
    override fun createIntent(context: Context, input: Args): Intent {
        return Intent(
            context,
            PaymentLauncherConfirmationActivity::class.java
        ).putExtras(input.toBundle())
    }

    override fun parseResult(resultCode: Int, intent: Intent?): InternalPaymentResult {
        return InternalPaymentResult.fromIntent(intent)
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    sealed class Args(
        open val publishableKey: String,
        open val stripeAccountId: String?,
        open val enableLogging: Boolean,
        open val productUsage: Set<String>,
        open val includePaymentSheetAuthenticators: Boolean,
        @ColorInt open var statusBarColor: Int? = null,
    ) : Parcelable {
        fun toBundle() = bundleOf(EXTRA_ARGS to this)

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class IntentConfirmationArgs internal constructor(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetAuthenticators: Boolean,
            val confirmStripeIntentParams: ConfirmStripeIntentParams,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetAuthenticators = includePaymentSheetAuthenticators,
            statusBarColor = statusBarColor,
        )

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class PaymentIntentNextActionArgs internal constructor(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetAuthenticators: Boolean,
            val paymentIntentClientSecret: String,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetAuthenticators = includePaymentSheetAuthenticators,
            statusBarColor = statusBarColor,
        )

        @Parcelize
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        data class SetupIntentNextActionArgs internal constructor(
            override val publishableKey: String,
            override val stripeAccountId: String?,
            override val enableLogging: Boolean,
            override val productUsage: Set<String>,
            override val includePaymentSheetAuthenticators: Boolean,
            val setupIntentClientSecret: String,
            @ColorInt override var statusBarColor: Int?,
        ) : Args(
            publishableKey = publishableKey,
            stripeAccountId = stripeAccountId,
            enableLogging = enableLogging,
            productUsage = productUsage,
            includePaymentSheetAuthenticators = includePaymentSheetAuthenticators,
            statusBarColor = statusBarColor,
        )

        internal companion object {
            private const val EXTRA_ARGS = "extra_args"

            fun fromIntent(intent: Intent): Args? {
                return intent.getParcelableExtra(EXTRA_ARGS)
            }
        }
    }
}
