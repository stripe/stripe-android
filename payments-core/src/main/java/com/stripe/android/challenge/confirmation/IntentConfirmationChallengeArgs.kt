package com.stripe.android.challenge.confirmation

import android.os.Parcelable
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.model.StripeIntent
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class IntentConfirmationChallengeArgs(
    val apiConfiguration: ApiConfiguration.State,
    val productUsage: List<String>,
    val intent: StripeIntent,
    val captchaVendorName: String?
) : Parcelable
