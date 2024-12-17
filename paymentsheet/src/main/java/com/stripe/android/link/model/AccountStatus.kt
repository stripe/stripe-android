package com.stripe.android.link.model

internal enum class AccountStatus {
    Verified, // Customer is signed in
    NeedsVerification, // Customer needs to authenticate
    VerificationStarted, // Customer has started OTP verification
    SignedOut, // Customer is signed out
    Error // Account status could not be determined
}
