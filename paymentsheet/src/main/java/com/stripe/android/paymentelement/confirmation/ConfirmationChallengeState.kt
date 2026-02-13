package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ConfirmationChallengeState(
    val hCaptchaToken: String? = null,
    val attestationToken: String? = null,
    val appId: String? = null,
    val passiveChallengeComplete: Boolean = false,
    val attestationComplete: Boolean = false,
) : Parcelable
