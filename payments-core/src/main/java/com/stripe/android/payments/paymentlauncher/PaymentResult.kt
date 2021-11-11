package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import android.os.Parcelable
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

/**
 * Result to be passed to the callback of [PaymentLauncher]
 */
sealed class PaymentResult : Parcelable {
    @Parcelize
    object Completed : PaymentResult()

    @Parcelize
    class Failed(val throwable: Throwable) : PaymentResult()

    @Parcelize
    object Canceled : PaymentResult()

    @JvmSynthetic
    fun toBundle() = bundleOf(EXTRA to this)

    internal companion object {
        private const val EXTRA = "extra_args"

        @JvmSynthetic
        fun fromIntent(intent: Intent?): PaymentResult {
            return intent?.getParcelableExtra(EXTRA)
                ?: Failed(IllegalStateException("Failed to get PaymentResult from Intent"))
        }
    }
}
