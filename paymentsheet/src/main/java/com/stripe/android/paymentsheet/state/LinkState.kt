package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import com.stripe.android.link.LinkPaymentLauncher
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class LinkState(
    val configuration: LinkPaymentLauncher.Configuration,
    val loginState: LoginState,
) : Parcelable {

    val isReadyForUse: Boolean
        get() = loginState in setOf(LoginState.LoggedIn, LoginState.NeedsVerification)

    enum class LoginState {
        LoggedIn, NeedsVerification, LoggedOut,
    }
}
