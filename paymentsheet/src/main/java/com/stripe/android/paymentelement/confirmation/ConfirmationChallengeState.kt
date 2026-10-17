package com.stripe.android.paymentelement.confirmation

import android.os.Parcelable
import com.stripe.android.model.AndroidVerificationObject
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class ConfirmationChallengeState(
    val hCaptchaToken: String? = null,
    val attestationResult: AndroidVerificationObject? = null,
    val passiveChallengeComplete: Boolean = false,
    val attestationComplete: Boolean = false,
) : Parcelable
