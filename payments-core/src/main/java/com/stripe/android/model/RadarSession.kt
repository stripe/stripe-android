package com.stripe.android.model

import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
data class RadarSession(
    val id: String,
    val passiveCaptchaSiteKey: String?,
    val passiveCaptchaRqdata: String?
) : StripeModel
