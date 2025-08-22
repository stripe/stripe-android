package com.stripe.android.link.model

import com.stripe.android.paymentsheet.state.LinkState

internal sealed interface AccountStatus {
    data class Verified(val consentPresentation: ConsentPresentation?) : AccountStatus // Customer is signed in
    data object NeedsVerification : AccountStatus // Customer needs to authenticate
    data object VerificationStarted : AccountStatus // Customer has started OTP verification
    data object SignedOut : AccountStatus // Customer is signed out
    data object Error : AccountStatus // Account status could not be determined
}

internal fun AccountStatus.toLoginState(): LinkState.LoginState {
    return when (this) {
        is AccountStatus.Verified ->
            LinkState.LoginState.LoggedIn
        AccountStatus.NeedsVerification,
        AccountStatus.VerificationStarted ->
            LinkState.LoginState.NeedsVerification
        AccountStatus.SignedOut,
        AccountStatus.Error ->
            LinkState.LoginState.LoggedOut
    }
}
