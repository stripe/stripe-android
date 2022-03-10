package com.stripe.android.identity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher

internal class StripeIdentityVerificationSheet(
    from: ComponentActivity,
    private val configuration: IdentityVerificationSheet.Configuration
) : IdentityVerificationSheet {

    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args> =
        from.registerForActivityResult(
            IdentityVerificationSheetContract(),
            ::onResult
        )

    private var onFinished: ((verificationResult: IdentityVerificationSheet.VerificationResult) -> Unit)? =
        null

    override fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String,
        onFinished: (verificationResult: IdentityVerificationSheet.VerificationResult) -> Unit
    ) {
        this.onFinished = onFinished
        activityResultLauncher.launch(
            IdentityVerificationSheetContract.Args(
                verificationSessionId,
                ephemeralKeySecret,
                configuration.merchantLogo
            )
        )
    }

    private fun onResult(verificationResult: IdentityVerificationSheet.VerificationResult) {
        onFinished?.let {
            it(verificationResult)
        }
    }
}
