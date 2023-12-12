package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.RadarSession
import org.json.JSONObject

internal class RadarSessionJsonParser : ModelJsonParser<RadarSession> {
    override fun parse(json: JSONObject): RadarSession? {
        return StripeJsonUtils.optString(json, FIELD_ID)?.let {
            RadarSession(
                id = it,
                passiveCaptchaSiteKey = StripeJsonUtils.optString(json, FIELD_PASSIVE_CAPTCHA_SITE_KEY),
                passiveCaptchaRqdata = StripeJsonUtils.optString(json, FIELD_PASSIVE_CAPTCHA_RQDATA)
            )
        }
    }

    private companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_PASSIVE_CAPTCHA_SITE_KEY = "passive_captcha_site_key"
        private const val FIELD_PASSIVE_CAPTCHA_RQDATA = "passive_captcha_rqdata"
    }
}
