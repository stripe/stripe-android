package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.StripeModel
import java.util.Objects
import org.json.JSONException
import org.json.JSONObject

/**
 * Represents an Ephemeral Key that can be used temporarily for API operations that typically
 * require a secret key.
 *
 * See [
 * Using Android Standard UI Components - Prepare your API](https://stripe.com/docs/mobile/android/standard#prepare-your-api) for more details on ephemeral keys.
 */
internal abstract class EphemeralKey : StripeModel, Parcelable {
    val objectId: String
    val created: Long
    val expires: Long
    val id: String
    val isLiveMode: Boolean
    val objectType: String
    val secret: String
    val type: String

    /**
     * Parcel constructor for this [Parcelable] class.
     * Note that if you change the order of these read values,
     * you must change the order in which they are written in
     * [.writeToParcel] below.
     *
     * @param `in` the [Parcel] in which this Ephemeral Key has been stored.
     */
    constructor(parcel: Parcel) {
        created = parcel.readLong()
        objectId = Objects.requireNonNull<String>(parcel.readString())
        expires = parcel.readLong()
        id = Objects.requireNonNull<String>(parcel.readString())
        isLiveMode = parcel.readInt() == 1
        objectType = Objects.requireNonNull<String>(parcel.readString())
        secret = Objects.requireNonNull<String>(parcel.readString())
        type = Objects.requireNonNull<String>(parcel.readString())
    }

    constructor(
        created: Long,
        objectId: String,
        expires: Long,
        id: String,
        liveMode: Boolean,
        objectType: String,
        secret: String,
        type: String
    ) {
        this.created = created
        this.objectId = objectId
        this.expires = expires
        this.id = id
        isLiveMode = liveMode
        this.objectType = objectType
        this.secret = secret
        this.type = type
    }

    override fun describeContents(): Int {
        return 0
    }

    /**
     * Write the object into a [Parcel]. Note that if the order of these write operations is
     * changed, an identical change must be made to [EphemeralKey] constructor above.
     *
     * @param out a [Parcel] into which to write this object
     * @param flags any flags (unused) for writing this object
     */
    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeLong(created)
        out.writeString(objectId)
        out.writeLong(expires)
        out.writeString(id)
        // There is no writeBoolean
        out.writeInt(if (isLiveMode) 1 else 0)
        out.writeString(objectType)
        out.writeString(secret)
        out.writeString(type)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        return if (other is EphemeralKey) {
            typedEquals(other)
        } else {
            false
        }
    }

    private fun typedEquals(ephemeralKey: EphemeralKey): Boolean {
        return (objectId == ephemeralKey.objectId &&
            created == ephemeralKey.created &&
            expires == ephemeralKey.expires &&
            id == ephemeralKey.id &&
            isLiveMode == ephemeralKey.isLiveMode &&
            objectType == ephemeralKey.objectType &&
            secret == ephemeralKey.secret &&
            type == ephemeralKey.type)
    }

    override fun hashCode(): Int {
        return Objects.hash(objectId, created, expires, id, isLiveMode, objectType, secret, type)
    }

    internal abstract class Factory<TEphemeralKey : EphemeralKey> {
        internal abstract fun create(
            created: Long,
            objectId: String,
            expires: Long,
            id: String,
            liveMode: Boolean,
            objectType: String,
            secret: String,
            type: String
        ): TEphemeralKey
    }

    companion object {
        private const val FIELD_CREATED = "created"
        private const val FIELD_EXPIRES = "expires"
        private const val FIELD_SECRET = "secret"
        private const val FIELD_LIVEMODE = "livemode"
        private const val FIELD_OBJECT = "object"
        private const val FIELD_ID = "id"
        private const val FIELD_ASSOCIATED_OBJECTS = "associated_objects"
        private const val FIELD_TYPE = "type"

        @Throws(JSONException::class)
        @JvmStatic
        fun <TEphemeralKey : EphemeralKey> fromJson(
            jsonObject: JSONObject,
            factory: Factory<TEphemeralKey>
        ): TEphemeralKey {
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

            return factory.create(created, objectId, expires, id, liveMode, objectType, secret, type)
        }
    }
}
