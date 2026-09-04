package com.stripe.android.challenge.passive

import android.os.Parcelable
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PassiveChallengeArgs(
    val passiveCaptchaParams: PassiveCaptchaParams,
    val apiConfiguration: ApiConfiguration.State,
    val productUsage: List<String>
) : Parcelable
