package com.stripe.android.model

import android.os.Parcelable
import androidx.annotation.StringDef
import com.stripe.android.model.parsers.SourceRedirectJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a [redirect](https://stripe.com/docs/api/sources/object#source_object-redirect) object
 * in the Sources API.
 */
@Parcelize
data class SourceRedirect internal constructor(
    val returnUrl: String?,
    @param:Status @field:Status @get:Status val status: String?,
    val url: String?
) : StripeModel(), Parcelable {

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
            return jsonObject?.let {
                SourceRedirectJsonParser().parse(it)
            }
        }
    }
}
