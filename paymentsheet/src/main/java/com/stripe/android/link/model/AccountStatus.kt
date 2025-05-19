package com.stripe.android.link.model

import com.stripe.android.paymentsheet.state.LinkState

internal enum class AccountStatus {
    Verified, // Customer is signed in
    NeedsVerification, // Customer needs to authenticate
    VerificationStarted, // Customer has started OTP verification
    SignedOut, // Customer is signed out
    Error // Account status could not be determined
}

internal fun AccountStatus.toLoginState(): LinkState.LoginState {
    return when (this) {
        AccountStatus.Verified ->
            LinkState.LoginState.LoggedIn
        AccountStatus.NeedsVerification,
        AccountStatus.VerificationStarted ->
            LinkState.LoginState.NeedsVerification
        AccountStatus.SignedOut,
        AccountStatus.Error ->
            LinkState.LoginState.LoggedOut
    }
}
