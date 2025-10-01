package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import androidx.annotation.VisibleForTesting
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.LinkSignupMode
import com.stripe.android.model.LinkDisabledReason
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

internal sealed interface LinkStateResult : Parcelable

@Parcelize
internal data class LinkState(
    val configuration: LinkConfiguration,
    val loginState: LoginState,
    val signupModeResult: LinkSignupModeResult
) : LinkStateResult {

    @VisibleForTesting
    constructor(
        configuration: LinkConfiguration,
        loginState: LoginState,
        signupMode: LinkSignupMode?
    ) : this(
        configuration = configuration,
        loginState = loginState,
        signupModeResult = when {
            signupMode != null ->
                LinkSignupModeResult.Enabled(signupMode)
            loginState != LoginState.LoggedOut ->
                LinkSignupModeResult.NotSignedOut
            else ->
                LinkSignupModeResult.Disabled(emptyList()) // Arbitrary reasons for testing.
        }
    )

    @IgnoredOnParcel
    val signupMode: LinkSignupMode? = signupModeResult.mode

    enum class LoginState {
        LoggedIn,
        NeedsVerification,
        NeedsWebVerification,
        LoggedOut,
    }
}

@Parcelize
internal data class LinkDisabledState(val linkDisabledReasons: List<LinkDisabledReason>) : LinkStateResult
