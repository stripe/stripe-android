package com.stripe.android.model.parsers

import com.stripe.android.core.model.StripeJsonUtils
import com.stripe.android.core.model.parsers.ModelJsonParser
import com.stripe.android.model.PassiveCaptchaParams
import org.json.JSONObject

internal class PassiveCaptchaJsonParser : ModelJsonParser<PassiveCaptchaParams> {
    override fun parse(json: JSONObject): PassiveCaptchaParams? {
        if (json.has(FIELD_SITE_KEY).not()) {
            return null
        }
        return PassiveCaptchaParams(
            siteKey = json.getString(FIELD_SITE_KEY),
            rqData = StripeJsonUtils.optString(json, FIELD_RQ_DATA)
        )
    }

    internal companion object {
        private const val FIELD_SITE_KEY = "site_key"
        private const val FIELD_RQ_DATA = "rqdata"
    }
}
