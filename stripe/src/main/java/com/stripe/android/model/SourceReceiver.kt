package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.model.parsers.SourceReceiverJsonParser
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Model for a
 * [receiver](https://stripe.com/docs/api/sources/object#source_object-receiver) object
 * in the Sources API. Present if the [Source] is a receiver.
 */
@Parcelize
data class SourceReceiver internal constructor(
    val address: String?,
    val amountCharged: Long,
    val amountReceived: Long,
    val amountReturned: Long
) : StripeModel(), Parcelable {

    companion object {
        @JvmStatic
        fun fromString(jsonString: String?): SourceReceiver? {
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
        fun fromJson(jsonObject: JSONObject?): SourceReceiver? {
            return jsonObject?.let {
                SourceReceiverJsonParser().parse(it)
            }
        }
    }
}
