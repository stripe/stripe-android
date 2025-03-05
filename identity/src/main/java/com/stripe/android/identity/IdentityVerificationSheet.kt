package com.stripe.android.identity

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import kotlinx.parcelize.Parcelize

interface IdentityVerificationSheet {
    /**
     * Result of verification.
     */
    sealed class VerificationFlowResult : Parcelable {
        @Parcelize
        data object Completed : VerificationFlowResult()

        @Parcelize
        data object Canceled : VerificationFlowResult()

        @Parcelize
        class Failed(val throwable: Throwable) : VerificationFlowResult()

        @JvmSynthetic
        fun toBundle() = bundleOf(EXTRA to this)

        internal companion object {
            internal const val EXTRA = "extra_args"

            fun fromIntent(intent: Intent?): VerificationFlowResult {
                return intent?.getParcelableExtra(EXTRA)
                    ?: Failed(IllegalStateException("Failed to get VerificationFlowResult from Intent"))
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
        fun onVerificationFlowResult(result: VerificationFlowResult)
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
            identityVerificationCallback: IdentityVerificationCallback
        ): IdentityVerificationSheet =
            StripeIdentityVerificationSheet(from, identityVerificationCallback)

        /**
         * Creates a [IdentityVerificationSheet] instance with [Fragment].
         *
         * This API registers an [ActivityResultLauncher] into the [Fragment] and notifies its
         * result to [identityVerificationCallback], it must be called before the [Fragment] is
         * created (in the onCreate method).
         */
        fun create(
            from: Fragment,
            identityVerificationCallback: IdentityVerificationCallback
        ): IdentityVerificationSheet =
            StripeIdentityVerificationSheet(from, identityVerificationCallback)

        /**
         * Creates a [IdentityVerificationSheet] instance in a [Composable]. Which would be
         * recreated if [configuration] or [identityVerificationCallback] changed.
         *
         * This API uses Compose specific API [rememberLauncherForActivityResult] to register a
         * [ActivityResultLauncher] into current activity, it should be called as part of Compose
         * initialization path.
         * The [IdentityVerificationSheet] created is remembered across recompositions.
         * Recomposition will always return the value produced by composition.
         */
        @Composable
        fun rememberIdentityVerificationSheet(
            identityVerificationCallback: IdentityVerificationCallback
        ): IdentityVerificationSheet {
            val context = LocalContext.current
            val activityResultLauncher = rememberLauncherForActivityResult(
                IdentityVerificationSheetContract(),
                identityVerificationCallback::onVerificationFlowResult
            )
            return StripeIdentityVerificationSheet(
                activityResultLauncher,
                context,
            )
        }
    }
}
