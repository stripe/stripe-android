package com.stripe.android.model

import android.os.Parcel
import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.StripeJsonUtils.optString
import java.util.Locale
import java.util.Objects
import org.json.JSONObject

/**
 * Model for an owner [address](https://stripe.com/docs/api#source_object-owner-address)
 * object in the Source api.
 */
class Address private constructor(
    val city: String?,
    val country: String?,
    val line1: String?,
    val line2: String?,
    val postalCode: String?,
    val state: String?
) : StripeModel(), StripeParamsModel, Parcelable {
    private constructor(addressBuilder: Builder) : this(
        addressBuilder.city,
        addressBuilder.country,
        addressBuilder.line1,
        addressBuilder.line2,
        addressBuilder.postalCode,
        addressBuilder.state
    )

    private constructor(parcel: Parcel) : this(
        city = parcel.readString(),
        country = parcel.readString(),
        line1 = parcel.readString(),
        line2 = parcel.readString(),
        postalCode = parcel.readString(),
        state = parcel.readString()
    )

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            FIELD_CITY to city.orEmpty(),
            FIELD_COUNTRY to country.orEmpty(),
            FIELD_LINE_1 to line1.orEmpty(),
            FIELD_LINE_2 to line2.orEmpty(),
            FIELD_POSTAL_CODE to postalCode.orEmpty(),
            FIELD_STATE to state.orEmpty()
        ).filterValues { it.isNotEmpty() }
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            other is Address -> typedEquals(other)
            else -> false
        }
    }

    private fun typedEquals(address: Address): Boolean {
        return city == address.city && country == address.country && line1 == address.line1 &&
            line2 == address.line2 && postalCode == address.postalCode && state == address.state
    }

    override fun hashCode(): Int {
        return Objects.hash(city, country, line1, line2, postalCode, state)
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        out.writeString(city)
        out.writeString(country)
        out.writeString(line1)
        out.writeString(line2)
        out.writeString(postalCode)
        out.writeString(state)
    }

    override fun describeContents(): Int {
        return 0
    }

    class Builder : ObjectBuilder<Address> {
        internal var city: String? = null
        internal var country: String? = null
        internal var line1: String? = null
        internal var line2: String? = null
        internal var postalCode: String? = null
        internal var state: String? = null

        fun setCity(city: String?): Builder {
            this.city = city
            return this
        }

        fun setCountry(country: String?): Builder {
            this.country = country?.toUpperCase(Locale.ROOT)
            return this
        }

        fun setLine1(line1: String?): Builder {
            this.line1 = line1
            return this
        }

        fun setLine2(line2: String?): Builder {
            this.line2 = line2
            return this
        }

        fun setPostalCode(postalCode: String?): Builder {
            this.postalCode = postalCode
            return this
        }

        fun setState(state: String?): Builder {
            this.state = state
            return this
        }

        override fun build(): Address {
            return Address(this)
        }
    }

    companion object {
        private const val FIELD_CITY = "city"
        // 2 Character Country Code
        private const val FIELD_COUNTRY = "country"
        private const val FIELD_LINE_1 = "line1"
        private const val FIELD_LINE_2 = "line2"
        private const val FIELD_POSTAL_CODE = "postal_code"
        private const val FIELD_STATE = "state"

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Address? {
            if (jsonObject == null) {
                return null
            }

            val city = optString(jsonObject, FIELD_CITY)
            val country = optString(jsonObject, FIELD_COUNTRY)
            val line1 = optString(jsonObject, FIELD_LINE_1)
            val line2 = optString(jsonObject, FIELD_LINE_2)
            val postalCode = optString(jsonObject, FIELD_POSTAL_CODE)
            val state = optString(jsonObject, FIELD_STATE)
            return Address(city, country, line1, line2, postalCode, state)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<Address> = object : Parcelable.Creator<Address> {
            override fun createFromParcel(parcel: Parcel): Address {
                return Address(parcel)
            }

            override fun newArray(size: Int): Array<Address?> {
                return arrayOfNulls(size)
            }
        }
    }
}
