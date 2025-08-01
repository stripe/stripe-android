package com.stripe.android.model

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object PassiveCaptchaParamsFactory {
    fun passiveCaptchaParams(
        siteKey: String = "2222-0000-ffff",
        rqData: String? = null,
    ): PassiveCaptchaParams {
        return PassiveCaptchaParams(
            siteKey = siteKey,
            rqData = rqData
        )
    }
}
