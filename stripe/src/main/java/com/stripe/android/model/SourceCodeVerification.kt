package com.stripe.android.model

import androidx.annotation.StringDef
import com.stripe.android.model.parsers.SourceCodeVerificationJsonParser
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
) : StripeModel {

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
        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): SourceCodeVerification? {
            return jsonObject?.let {
                SourceCodeVerificationJsonParser().parse(it)
            }
        }
    }
}
