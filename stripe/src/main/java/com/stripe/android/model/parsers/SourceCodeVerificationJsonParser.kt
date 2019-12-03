package com.stripe.android.model.parsers

import com.stripe.android.model.SourceCodeVerification
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

internal class SourceCodeVerificationJsonParser : ModelJsonParser<SourceCodeVerification> {
    override fun parse(json: JSONObject): SourceCodeVerification {
        return SourceCodeVerification(
            json.optInt(FIELD_ATTEMPTS_REMAINING, INVALID_ATTEMPTS_REMAINING),
            asStatus(optString(json, FIELD_STATUS))
        )
    }

    private companion object {
        private const val FIELD_ATTEMPTS_REMAINING = "attempts_remaining"
        private const val FIELD_STATUS = "status"
        private const val INVALID_ATTEMPTS_REMAINING = -1

        @SourceCodeVerification.Status
        private fun asStatus(stringStatus: String?): String? {
            return when (stringStatus) {
                SourceCodeVerification.Status.PENDING -> SourceCodeVerification.Status.PENDING
                SourceCodeVerification.Status.SUCCEEDED -> SourceCodeVerification.Status.SUCCEEDED
                SourceCodeVerification.Status.FAILED -> SourceCodeVerification.Status.FAILED
                else -> null
            }
        }
    }
}
