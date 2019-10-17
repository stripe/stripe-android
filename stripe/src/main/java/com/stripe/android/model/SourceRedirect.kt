package com.stripe.android.model

import androidx.annotation.StringDef
import androidx.annotation.VisibleForTesting
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a [redirect](https://stripe.com/docs/api/sources/object#source_object-redirect) object
 * in the Sources API.
 */
data class SourceRedirect private constructor(
    val returnUrl: String?,
    @param:Status @field:Status @get:Status val status: String?,
    val url: String?
) : StripeModel() {

    @Retention(AnnotationRetention.SOURCE)
    @StringDef(Status.PENDING, Status.SUCCEEDED, Status.FAILED, Status.NOT_REQUIRED)
    internal annotation class Status {
        companion object {
            const val PENDING = "pending"
            const val SUCCEEDED = "succeeded"
            const val FAILED = "failed"
            const val NOT_REQUIRED = "not_required"
        }
    }

    companion object {
        private const val FIELD_RETURN_URL = "return_url"
        private const val FIELD_STATUS = "status"
        private const val FIELD_URL = "url"

        @JvmStatic
        fun fromString(jsonString: String?): SourceRedirect? {
            if (jsonString == null) {
                return null
            }

            return try {
                fromJson(JSONObject(jsonString))
            } catch (ignored: JSONException) {
                null
            }
        }

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceRedirect? {
            if (jsonObject == null) {
                return null
            }

            val returnUrl = optString(jsonObject, FIELD_RETURN_URL)
            @Status val status = asStatus(optString(jsonObject, FIELD_STATUS))
            val url = optString(jsonObject, FIELD_URL)
            return SourceRedirect(returnUrl, status, url)
        }

        @Status
        @VisibleForTesting
        internal fun asStatus(stringStatus: String?): String? {
            return when (stringStatus) {
                Status.PENDING -> Status.PENDING
                Status.SUCCEEDED -> Status.SUCCEEDED
                Status.FAILED -> Status.FAILED
                Status.NOT_REQUIRED -> Status.NOT_REQUIRED
                else -> null
            }
        }
    }
}
