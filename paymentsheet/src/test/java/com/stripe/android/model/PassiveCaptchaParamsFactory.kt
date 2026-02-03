package com.stripe.android.model

internal object PassiveCaptchaParamsFactory {
    fun passiveCaptchaParams(
        siteKey: String = "2222-0000-ffff",
        rqData: String? = "test_rq_data",
        tokenTimeoutSeconds: Int? = 30,
    ): PassiveCaptchaParams {
        return PassiveCaptchaParams(
            siteKey = siteKey,
            rqData = rqData,
            tokenTimeoutSeconds = tokenTimeoutSeconds
        )
    }
}
