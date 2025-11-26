package com.stripe.android.challenge.confirmation

import android.os.Parcelable
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class IntentConfirmationChallengeArgs(
    val publishableKey: String,
    val productUsage: List<String>,
    val intent: StripeIntent
) : Parcelable
