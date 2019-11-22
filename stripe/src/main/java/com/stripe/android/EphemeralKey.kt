package com.stripe.android

import android.os.Parcelable
import com.stripe.android.model.StripeModel
import kotlinx.android.parcel.Parcelize
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents an Ephemeral Key that can be used temporarily for API operations that typically
 * require a secret key.
 *
 * See [Using Android Standard UI Components - Prepare your API](https://stripe.com/docs/mobile/android/standard#prepare-your-api)
 * for more details on ephemeral keys.
 */
@Parcelize
data class EphemeralKey internal constructor(
    /**
     * Represents a customer id or issuing card id, depending on the context
     */
    internal val objectId: String,

    internal val created: Long,
    internal val expires: Long,
    internal val id: String,
    internal val isLiveMode: Boolean,
    internal val objectType: String,
    val secret: String,
    internal val type: String
) : StripeModel(), Parcelable {

    internal companion object {
        private const val FIELD_CREATED = "created"
        private const val FIELD_EXPIRES = "expires"
        private const val FIELD_SECRET = "secret"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ID = "id"
        private const val FIELD_ASSOCIATED_OBJECTS = "associated_objects"
        private const val FIELD_TYPE = "type"

        @Throws(JSONException::class)
        @JvmSynthetic
        internal fun fromJson(jsonObject: JSONObject): EphemeralKey {
            val created = jsonObject.getLong(FIELD_CREATED)
            val expires = jsonObject.getLong(FIELD_EXPIRES)
            val id = jsonObject.getString(FIELD_ID)
            val liveMode = jsonObject.getBoolean(FIELD_LIVEMODE)
            val objectType = jsonObject.getString(FIELD_OBJECT)
            val secret = jsonObject.getString(FIELD_SECRET)

            // Get the values from the associated objects array first element
            val associatedObjectArray = jsonObject.getJSONArray(FIELD_ASSOCIATED_OBJECTS)
            val typeObject = associatedObjectArray.getJSONObject(0)
            val type = typeObject.getString(FIELD_TYPE)
            val objectId = typeObject.getString(FIELD_ID)

            return EphemeralKey(
                objectId = objectId,
                created = created,
                expires = expires,
                id = id,
                isLiveMode = liveMode,
                objectType = objectType,
                secret = secret,
                type = type
            )
        }
    }
}
