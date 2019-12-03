package com.stripe.android.model.parsers

import androidx.annotation.VisibleForTesting
import com.stripe.android.model.SourceRedirect
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class SourceRedirectJsonParser : ModelJsonParser<SourceRedirect> {
    override fun parse(json: JSONObject): SourceRedirect {
        return SourceRedirect(
            returnUrl = optString(json, FIELD_RETURN_URL),
            status = asStatus(optString(json, FIELD_STATUS)),
            url = optString(json, FIELD_URL)
        )
    }

    internal companion object {
        private const val FIELD_RETURN_URL = "return_url"
        private const val FIELD_STATUS = "status"
        private const val FIELD_URL = "url"

        @SourceRedirect.Status
        @VisibleForTesting
        internal fun asStatus(stringStatus: String?): String? {
            return when (stringStatus) {
                SourceRedirect.Status.PENDING -> SourceRedirect.Status.PENDING
                SourceRedirect.Status.SUCCEEDED -> SourceRedirect.Status.SUCCEEDED
                SourceRedirect.Status.FAILED -> SourceRedirect.Status.FAILED
                SourceRedirect.Status.NOT_REQUIRED -> SourceRedirect.Status.NOT_REQUIRED
                else -> null
            }
        }
    }
}
