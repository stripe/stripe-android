package com.stripe.android.identity

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.parcelize.Parcelize

interface IdentityVerificationSheet {
    /**
     * Data to configure the verification flow.
     */
    data class Configuration(
        /**
         * Brand logo to display on the consent page of verification flow.
         * The Uri could be a local drawable resource file or a web image.
         * The logo will be displayed in a 32x32 dp ImageView.
         */
        val brandLogo: Uri
    )

    /**
     * Result of verification.
     */
    sealed class VerificationFlowResult : Parcelable {
        @Parcelize
        object Completed : VerificationFlowResult()

        @Parcelize
        object Canceled : VerificationFlowResult()

        @Parcelize
        class Failed(val throwable: Throwable) : VerificationFlowResult()

        @JvmSynthetic
        fun toBundle() = bundleOf(EXTRA to this)

        internal companion object {
            private const val EXTRA = "extra_args"

            fun fromIntent(intent: Intent?): VerificationFlowResult {
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
        ephemeralKeySecret: String
    )

    /**
     * Callback to notify when identity verification finishes and a result is available.
     */
    fun interface IdentityVerificationCallback {
        fun onVerificationResult(result: VerificationFlowResult)
    }

    companion object {
        /**
         * Creates a [IdentityVerificationSheet] instance with [ComponentActivity].
         *
         * This API registers an [ActivityResultLauncher] into the
         * [ComponentActivity] and notifies its result to [identityVerificationCallback], it must
         * be called before the [ComponentActivity]
         * is created (in the onCreate method).
         */
        fun create(
            from: ComponentActivity,
            configuration: Configuration,
            identityVerificationCallback: IdentityVerificationCallback
        ): IdentityVerificationSheet =
            StripeIdentityVerificationSheet(from, configuration, identityVerificationCallback)

        /**
         * Creates a [IdentityVerificationSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment] and notifies its
         * result to [identityVerificationCallback], it must be called before the [Fragment] is
         * created (in the onCreate method).
         */
        fun create(
            from: Fragment,
            configuration: Configuration,
            identityVerificationCallback: IdentityVerificationCallback
        ): IdentityVerificationSheet =
            StripeIdentityVerificationSheet(from, configuration, identityVerificationCallback)
    }
}
