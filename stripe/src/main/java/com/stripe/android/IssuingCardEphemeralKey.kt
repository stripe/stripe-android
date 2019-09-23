package com.stripe.android

import android.os.Parcel
import android.os.Parcelable

internal class IssuingCardEphemeralKey : EphemeralKey {

    val issuingCardId: String
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

    internal class Factory : EphemeralKey.Factory<IssuingCardEphemeralKey>() {
        override fun create(
            created: Long,
            objectId: String,
            expires: Long,
            id: String,
            liveMode: Boolean,
            objectType: String,
            secret: String,
            type: String
        ): IssuingCardEphemeralKey {
            return IssuingCardEphemeralKey(
                created, objectId, expires, id, liveMode, objectType, secret, type
            )
        }
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<IssuingCardEphemeralKey> =
            object : Parcelable.Creator<IssuingCardEphemeralKey> {
                override fun createFromParcel(parcel: Parcel): IssuingCardEphemeralKey {
                    return IssuingCardEphemeralKey(parcel)
                }

                override fun newArray(size: Int): Array<IssuingCardEphemeralKey?> {
                    return arrayOfNulls(size)
                }
            }
    }
}
