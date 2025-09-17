package com.stripe.android.link.model

import com.stripe.android.paymentsheet.state.LinkState

internal sealed interface AccountStatus {
    data class Verified(
        val hasVerifiedSMSSession: Boolean,
        val consentPresentation: ConsentPresentation?,
    ) : AccountStatus // Customer is signed in
    data class NeedsVerification(
        val webviewOpenUrl: String? = null,
    ) : AccountStatus // Customer needs to authenticate
    data object VerificationStarted : AccountStatus // Customer has started OTP verification
    data object SignedOut : AccountStatus // Customer is signed out
    data class Error(val error: Throwable) : AccountStatus // Account status could not be determined
}

internal fun AccountStatus.toLoginState(): LinkState.LoginState {
    return when (this) {
        is AccountStatus.Verified ->
            LinkState.LoginState.LoggedIn
        is AccountStatus.NeedsVerification,
        AccountStatus.VerificationStarted ->
            LinkState.LoginState.NeedsVerification
        AccountStatus.SignedOut,
        is AccountStatus.Error ->
            LinkState.LoginState.LoggedOut
    }
}
