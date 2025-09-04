package com.stripe.android.payments.core.authentication.challenge

import android.os.Parcelable
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class IntentConfirmationChallengeArgs(
    val intentConfirmationChallenge: StripeIntent.NextActionData.SdkData.IntentConfirmationChallenge,
    val publishableKey: String,
    val productUsage: List<String>
) : Parcelable
