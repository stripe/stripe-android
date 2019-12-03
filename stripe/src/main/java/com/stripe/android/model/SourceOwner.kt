package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.parsers.SourceOwnerJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a [owner](https://stripe.com/docs/api#source_object-owner) object
 * in the Source api.
 */
@Parcelize
data class SourceOwner internal constructor(
    val address: Address?,
    val email: String?,
    val name: String?,
    val phone: String?,
    val verifiedAddress: Address?,
    val verifiedEmail: String?,
    val verifiedName: String?,
    val verifiedPhone: String?
) : StripeModel(), Parcelable {
    companion object {
        @JvmStatic
        fun fromString(jsonString: String?): SourceOwner? {
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
        fun fromJson(jsonObject: JSONObject?): SourceOwner? {
            return jsonObject?.let {
                SourceOwnerJsonParser().parse(it)
            }
        }
    }
}
