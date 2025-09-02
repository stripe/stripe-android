package com.stripe.android.payments.core.authentication.challenge

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
sealed interface IntentConfirmationChallengeActivityResult : Parcelable {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Success(val token: String) : IntentConfirmationChallengeActivityResult

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Parcelize
    data class Failed(val error: Throwable) : IntentConfirmationChallengeActivityResult
}
