package com.stripe.android.stripe3ds2.transaction

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed class ChallengeAction : Parcelable {
    @Parcelize
    data class NativeForm(
        internal val userEntry: String,
        internal val whitelistingValue: Boolean
    ) : ChallengeAction()

    @Parcelize
    data class HtmlForm(
        internal val userEntry: String
    ) : ChallengeAction()

    @Parcelize
    data class Oob(
        internal val whitelistingValue: Boolean
    ) : ChallengeAction()

    @Parcelize
    object Resend : ChallengeAction()

    @Parcelize
    object Cancel : ChallengeAction()
}
