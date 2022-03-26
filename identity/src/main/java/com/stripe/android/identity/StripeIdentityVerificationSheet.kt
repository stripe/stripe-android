package com.stripe.android.identity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

internal class StripeIdentityVerificationSheet private constructor(
    activityResultCaller: ActivityResultCaller,
    private val configuration: IdentityVerificationSheet.Configuration,
    onFinished: (verificationResult: IdentityVerificationSheet.VerificationResult) -> Unit
) : IdentityVerificationSheet {

    constructor(
        from: ComponentActivity,
        configuration: IdentityVerificationSheet.Configuration,
        onFinished: (verificationResult: IdentityVerificationSheet.VerificationResult) -> Unit
    ) : this(from as ActivityResultCaller, configuration, onFinished)

    constructor(
        from: Fragment,
        configuration: IdentityVerificationSheet.Configuration,
        onFinished: (verificationResult: IdentityVerificationSheet.VerificationResult) -> Unit
    ) : this(from as ActivityResultCaller, configuration, onFinished)

    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
            IdentityVerificationSheetContract(),
            onFinished
        )

    override fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String,
    ) {
        activityResultLauncher.launch(
            IdentityVerificationSheetContract.Args(
                verificationSessionId,
                ephemeralKeySecret,
                configuration.brandLogo
            )
        )
    }
}
