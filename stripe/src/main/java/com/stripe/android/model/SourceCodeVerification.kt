package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.StringDef
import com.stripe.android.model.StripeJsonUtils.optString
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model for a [code verification](https://stripe.com/docs/api/sources/object#source_object-code_verification)
 * object in the Sources API.
 *
 * *Not* source code verification.
 */
@Parcelize
data class SourceCodeVerification internal constructor(
    val attemptsRemaining: Int,
    @param:Status @field:Status @get:Status val status: String?
) : StripeModel(), Parcelable {

    // Note: these are the same as the values for the @Redirect.Status StringDef.
    // They don't have to stay the same forever, so they are redefined here.
    @Retention(AnnotationRetention.SOURCE)
    @StringDef(Status.PENDING, Status.SUCCEEDED, Status.FAILED)
    internal annotation class Status {
        companion object {
            const val PENDING = "pending"
            const val SUCCEEDED = "succeeded"
            const val FAILED = "failed"
        }
    }

    companion object {
        private const val FIELD_ATTEMPTS_REMAINING = "attempts_remaining"
        private const val FIELD_STATUS = "status"
        private const val INVALID_ATTEMPTS_REMAINING = -1

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceCodeVerification? {
            return if (jsonObject == null) {
                null
            } else SourceCodeVerification(
                jsonObject.optInt(FIELD_ATTEMPTS_REMAINING, INVALID_ATTEMPTS_REMAINING),
                asStatus(optString(jsonObject, FIELD_STATUS))
            )
        }

        @Status
        private fun asStatus(stringStatus: String?): String? {
            return when (stringStatus) {
                Status.PENDING -> Status.PENDING
                Status.SUCCEEDED -> Status.SUCCEEDED
                Status.FAILED -> Status.FAILED
                else -> null
            }
        }
    }
}
