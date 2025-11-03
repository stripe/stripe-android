package com.stripe.android.challenge.confirmation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

internal sealed interface IntentConfirmationChallengeActivityResult : Parcelable {
    @Parcelize
    data class Success(val clientSecret: String) : IntentConfirmationChallengeActivityResult

    @Parcelize
    data class Failed(val error: Throwable) : IntentConfirmationChallengeActivityResult
}
