package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import com.stripe.android.StripeIntentResult
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

/**
 * Result to be passed to the callback of [PaymentLauncher]
 */
sealed class PaymentResult : Parcelable {
    @Parcelize
    class Completed(
        val intentResult: StripeIntentResult<StripeIntent>
    ) : PaymentResult()

    @Parcelize
    object Failed : PaymentResult() // default if no extra found

    @Parcelize
    object Canceled : PaymentResult()

    @JvmSynthetic
    fun toBundle() = bundleOf(EXTRA to this)

    internal companion object {
        private const val EXTRA = "extra_args"

        @JvmSynthetic
        fun fromIntent(intent: Intent?): PaymentResult {
            return intent?.getParcelableExtra(EXTRA) ?: Failed
        }
    }
}
