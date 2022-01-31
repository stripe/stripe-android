package com.stripe.android.identity

class StripeIdentityVerificationSheet : IdentityVerificationSheet {
    override fun present(
        verificationSessionId: String,
        ephemeralKeySecret: String,
        onFinished: (verificationResult: IdentityVerificationSheet.VerificationResult) -> Unit
    ) {
        onFinished(IdentityVerificationSheet.VerificationResult.Completed)
    }
}
