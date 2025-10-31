package com.stripe.android.challenge.confirmation

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface IntentConfirmationChallengeActivityResult : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Success(val clientSecret: String) : IntentConfirmationChallengeActivityResult

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Failed(val error: Throwable) : IntentConfirmationChallengeActivityResult
}
