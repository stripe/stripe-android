package com.stripe.android.model

internal object PassiveCaptchaParamsFactory {
    fun passiveCaptchaParams(
        siteKey: String = "2222-0000-ffff",
        rqData: String? = "test_rq_data",
    ): PassiveCaptchaParams {
        return PassiveCaptchaParams(
            siteKey = siteKey,
            rqData = rqData
        )
    }
}
