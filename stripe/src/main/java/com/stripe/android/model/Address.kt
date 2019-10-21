package com.stripe.android.model

import android.os.Parcelable
import com.stripe.android.ObjectBuilder
import com.stripe.android.model.StripeJsonUtils.optString
import java.util.Locale
import kotlinx.android.parcel.Parcelize
import org.json.JSONObject

/**
 * Model for an owner [address](https://stripe.com/docs/api#source_object-owner-address)
 * object in the Source api.
 */
@Parcelize
data class Address internal constructor(
    val city: String?,
    val country: String?,
    val line1: String?,
    val line2: String?,
    val postalCode: String?,
    val state: String?
) : StripeModel(), StripeParamsModel, Parcelable {

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

    class Builder : ObjectBuilder<Address> {
        private var city: String? = null
        private var country: String? = null
        private var line1: String? = null
        private var line2: String? = null
        private var postalCode: String? = null
        private var state: String? = null

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
            return Address(
                city = city,
                country = country,
                line1 = line1,
                line2 = line2,
                postalCode = postalCode,
                state = state
            )
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
    }
}
