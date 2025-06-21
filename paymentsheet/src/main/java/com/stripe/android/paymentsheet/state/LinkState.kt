package com.stripe.android.paymentsheet.state

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.link.LinkConfiguration
import com.stripe.android.link.ui.inline.LinkSignupMode
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class LinkState(
    val configuration: LinkConfiguration,
    val loginState: LoginState,
    val signupMode: LinkSignupMode?,
) : Parcelable {

    enum class LoginState {
        LoggedIn, NeedsVerification, LoggedOut,
    }
}
