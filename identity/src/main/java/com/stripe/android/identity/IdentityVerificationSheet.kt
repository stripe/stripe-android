package com.stripe.android.identity

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import kotlinx.parcelize.Parcelize

interface IdentityVerificationSheet {
    /**
     * Data to configure the verification flow.
     */
    data class Configuration(
        @DrawableRes
        val merchantLogo: Int,
        val stripePublishableKey: String
    )

    /**
     * Result of verification.
     */
    sealed class VerificationResult : Parcelable {
        @Parcelize
        object Completed : VerificationResult()

        @Parcelize
        object Canceled : VerificationResult()

        @Parcelize
        class Failed(val throwable: Throwable) : VerificationResult()


        @JvmSynthetic
        fun toBundle() = bundleOf(EXTRA to this)

        internal companion object {
            private const val EXTRA = "extra_args"

            fun fromIntent(intent: Intent?): VerificationResult {
                return intent?.getParcelableExtra(EXTRA)
                    ?: Failed(IllegalStateException("Failed to get VerificationResult from Intent"))
            }
        }

    }

    /**
     * Start the verification flow
     */
    fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String,
        onFinished: (verificationResult: VerificationResult) -> Unit
    )

    companion object {
        /**
         * Factory method to create a [IdentityVerificationSheet] instance.
         */
        fun create(
            from: ComponentActivity,
            configuration: Configuration,
        ): IdentityVerificationSheet = StripeIdentityVerificationSheet(from, configuration)
    }
}
