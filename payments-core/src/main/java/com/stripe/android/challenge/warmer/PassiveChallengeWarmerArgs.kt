package com.stripe.android.challenge.warmer

import android.os.Parcelable
import com.stripe.android.model.PassiveCaptchaParams
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class PassiveChallengeWarmerArgs(
    val passiveCaptchaParams: PassiveCaptchaParams
) : Parcelable
