package com.stripe.android.identity

import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment

internal class StripeIdentityVerificationSheet private constructor(
    activityResultCaller: ActivityResultCaller,
    private val configuration: IdentityVerificationSheet.Configuration,
    identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
) : IdentityVerificationSheet {

    constructor(
        from: ComponentActivity,
        configuration: IdentityVerificationSheet.Configuration,
        identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
    ) : this(from as ActivityResultCaller, configuration, identityVerificationCallback)

    constructor(
        from: Fragment,
        configuration: IdentityVerificationSheet.Configuration,
        identityVerificationCallback: IdentityVerificationSheet.IdentityVerificationCallback
    ) : this(from as ActivityResultCaller, configuration, identityVerificationCallback)

    private val activityResultLauncher: ActivityResultLauncher<IdentityVerificationSheetContract.Args> =
        activityResultCaller.registerForActivityResult(
            IdentityVerificationSheetContract(),
            identityVerificationCallback::onVerificationResult
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
