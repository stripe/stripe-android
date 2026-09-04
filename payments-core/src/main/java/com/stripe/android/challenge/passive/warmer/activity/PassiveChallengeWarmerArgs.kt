package com.stripe.android.challenge.passive.warmer.activity

import android.os.Parcelable
import com.stripe.android.core.ApiConfiguration
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PassiveChallengeWarmerArgs(
    val passiveCaptchaParams: PassiveCaptchaParams,
    val apiConfiguration: ApiConfiguration.State,
    val productUsage: List<String>
) : Parcelable
