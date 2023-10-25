package com.stripe.android.payments.paymentlauncher

import android.content.Intent
import android.os.Parcelable
import androidx.annotation.RestrictTo
import androidx.core.os.bundleOf
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed class PaymentLauncherResult : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Completed(val intent: StripeIntent) : PaymentLauncherResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    class Failed(val throwable: Throwable) : PaymentLauncherResult()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    object Canceled : PaymentLauncherResult()

    @JvmSynthetic
    internal fun toBundle() = bundleOf(EXTRA to this)

    internal companion object {
        private const val EXTRA = "extra_args"

        @JvmSynthetic
        fun fromIntent(intent: Intent?): PaymentLauncherResult {
            return intent?.getParcelableExtra(EXTRA)
                ?: Failed(IllegalStateException("Failed to get PaymentSheetResult from Intent"))
        }
    }
}
