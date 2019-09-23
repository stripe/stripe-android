package com.stripe.android

import android.os.Parcel
import android.os.Parcelable
import org.json.JSONException
import org.json.JSONObject

internal class CustomerEphemeralKey : EphemeralKey {
    val customerId: String
        get() = objectId

    private constructor(parcel: Parcel) : super(parcel)

    private constructor(
        created: Long,
        customerId: String,
        expires: Long,
        id: String,
        liveMode: Boolean,
        objectType: String,
        secret: String,
        type: String
    ) : super(created, customerId, expires, id, liveMode, objectType, secret, type)

    internal class Factory : EphemeralKey.Factory<CustomerEphemeralKey>() {
        override fun create(
            created: Long,
            objectId: String,
            expires: Long,
            id: String,
            liveMode: Boolean,
            objectType: String,
            secret: String,
            type: String
        ): CustomerEphemeralKey {
            return CustomerEphemeralKey(
                created, objectId, expires, id, liveMode, objectType, secret, type
            )
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<CustomerEphemeralKey> =
            object : Parcelable.Creator<CustomerEphemeralKey> {

                override fun createFromParcel(`in`: Parcel): CustomerEphemeralKey {
                    return CustomerEphemeralKey(`in`)
                }

                override fun newArray(size: Int): Array<CustomerEphemeralKey?> {
                    return arrayOfNulls(size)
                }
            }

        @Throws(JSONException::class)
        @JvmStatic
        fun fromJson(jsonObject: JSONObject): CustomerEphemeralKey {
            return fromJson(jsonObject, Factory())
        }
    }
}
