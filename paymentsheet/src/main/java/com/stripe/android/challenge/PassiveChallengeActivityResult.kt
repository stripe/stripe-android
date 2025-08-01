package com.stripe.android.challenge

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface PassiveChallengeActivityResult : Parcelable {
    @Parcelize
    data class Success(val token: String) : PassiveChallengeActivityResult

    @Parcelize
    data class Failed(val error: Throwable) : PassiveChallengeActivityResult
}
