package com.stripe.android.identity

import android.content.Intent
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.parcelize.Parcelize

interface IdentityVerificationSheet {
    /**
     * Data to configure the verification flow.
     */
    data class Configuration(
        @DrawableRes
        val merchantLogo: Int
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
     * Starts the verification flow.
     */
    fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String,
        onFinished: (verificationResult: VerificationResult) -> Unit
    )

    companion object {
        /**
         * Creates a [IdentityVerificationSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity], it must be called before the [ComponentActivity]
         * is created (in the onCreate method).
         */
        fun create(
            from: ComponentActivity,
            configuration: Configuration,
        ): IdentityVerificationSheet = StripeIdentityVerificationSheet(from, configuration)

        /**
         * Creates a [IdentityVerificationSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment], it must be called
         * before the [Fragment] is created (in the onCreate method).
         */
        fun create(
            from: Fragment,
            configuration: Configuration,
        ): IdentityVerificationSheet = StripeIdentityVerificationSheet(from, configuration)
    }
}
