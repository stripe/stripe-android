package com.stripe.android.identity

import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.annotation.DrawableRes
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
    sealed interface VerificationResult : Parcelable {
        @Parcelize
        object Completed : VerificationResult

        @Parcelize
        object Canceled : VerificationResult

        @Parcelize
        class Failed(val throwable: Throwable) : VerificationResult
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
        ): IdentityVerificationSheet = StripeIdentityVerificationSheet()
    }
}
