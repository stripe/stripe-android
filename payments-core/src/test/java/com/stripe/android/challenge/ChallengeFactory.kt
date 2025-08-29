package com.stripe.android.challenge

import com.stripe.android.model.PassiveCaptchaParams

internal object ChallengeFactory {
    fun passiveCaptchaParams(
        siteKey: String = PASSIVE_CAPTCHA_SITE_KEY,
        rqData: String = PASSIVE_CAPTCHA_RQ_DATA
    ) = PassiveCaptchaParams(
        siteKey = siteKey,
        rqData = rqData
    )

    const val PASSIVE_CAPTCHA_SITE_KEY = "test_site_key"
    const val PASSIVE_CAPTCHA_RQ_DATA = "test_rq_data"
}
