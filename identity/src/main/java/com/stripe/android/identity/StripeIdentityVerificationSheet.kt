package com.stripe.android.identity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

internal class StripeIdentityVerificationSheet private constructor(
    activityResultCaller: ActivityResultCaller,
    private val configuration: IdentityVerificationSheet.Configuration
) : IdentityVerificationSheet {

    constructor(
        from: ComponentActivity,
        configuration: IdentityVerificationSheet.Configuration
    ) : this(from as ActivityResultCaller, configuration)

    constructor(
        from: Fragment,
        configuration: IdentityVerificationSheet.Configuration
    ) : this(from as ActivityResultCaller, configuration)

    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
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
                configuration.brandLogo
            )
        )
    }

    private fun onResult(verificationResult: IdentityVerificationSheet.VerificationResult) {
        onFinished?.let {
            it(verificationResult)
        }
    }
}
