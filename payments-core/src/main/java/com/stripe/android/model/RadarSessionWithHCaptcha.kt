package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.StripeModel
import kotlinx.parcelize.Parcelize

@Parcelize
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class RadarSessionWithHCaptcha(
    val id: String,
    val passiveCaptchaSiteKey: String?,
    val passiveCaptchaRqdata: String?
) : StripeModel
