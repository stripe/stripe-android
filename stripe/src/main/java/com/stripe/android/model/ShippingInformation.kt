package com.stripe.android.model

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.model.StripeJsonUtils.optString
import org.json.JSONObject

/**
 * Model representing a shipping address object
 */
data class ShippingInformation constructor(
    val address: Address?,
    val name: String?,
    val phone: String?
) : StripeModel(), StripeParamsModel, Parcelable {

    private constructor(parcel: Parcel) : this(
        parcel.readParcelable(Address::class.java.classLoader),
        parcel.readString(),
        parcel.readString()
    )

    override fun toParamMap(): Map<String, Any> {
        return listOf(
            FIELD_NAME to name,
            FIELD_PHONE to phone,
            FIELD_ADDRESS to address?.toParamMap()
        )
            .mapNotNull { pair -> pair.second?.let { Pair(pair.first, it) } }
            .toMap()
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(address, flags)
        dest.writeString(name)
        dest.writeString(phone)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<ShippingInformation> =
            object : Parcelable.Creator<ShippingInformation> {
                override fun createFromParcel(source: Parcel): ShippingInformation {
                    return ShippingInformation(source)
                }

                override fun newArray(size: Int): Array<ShippingInformation?> {
                    return arrayOfNulls(size)
                }
            }

        private const val FIELD_ADDRESS = "address"
        private const val FIELD_NAME = "name"
        private const val FIELD_PHONE = "phone"

        fun fromJson(jsonObject: JSONObject?): ShippingInformation? {
            return if (jsonObject == null) {
                null
            } else ShippingInformation(
                Address.fromJson(jsonObject.optJSONObject(FIELD_ADDRESS)),
                optString(jsonObject, FIELD_NAME),
                optString(jsonObject, FIELD_PHONE)
            )
        }
    }
}
