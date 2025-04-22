package com.stripe.android.model

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryCode
import com.stripe.android.core.model.StripeModel
import com.stripe.android.model.parsers.AddressJsonParser
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

/**
 * Model for an owner [address](https://stripe.com/docs/api#source_object-owner-address)
 * object in the Source api.
 */
@Parcelize
data class Address(
    val city: String? = null,
    val country: String? = null, // two-character country code
    val line1: String? = null,
    val line2: String? = null,
    val postalCode: String? = null,
    val state: String? = null
) : StripeModel, StripeParamsModel {
    internal val countryCode: CountryCode?
        get() = country?.takeUnless { it.isBlank() }?.let { CountryCode.create(it) }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
    fun getCountryCode() = countryCode

    override fun toParamMap(): Map<String, Any> {
        return mapOf(
            PARAM_CITY to city.orEmpty(),
            PARAM_COUNTRY to country.orEmpty(),
            PARAM_LINE_1 to line1.orEmpty(),
            PARAM_LINE_2 to line2.orEmpty(),
            PARAM_POSTAL_CODE to postalCode.orEmpty(),
            PARAM_STATE to state.orEmpty()
        ).filterValues { it.isNotEmpty() }
    }

    internal fun isFilledOut(): Boolean {
        return city != null || country != null || line1 != null || line2 != null || postalCode != null || state != null
    }

    class Builder {
        private var city: String? = null
        private var country: String? = null
        private var line1: String? = null
        private var line2: String? = null
        private var postalCode: String? = null
        private var state: String? = null

        fun setCity(city: String?): Builder = apply {
            this.city = city
        }

        fun setCountry(country: String?): Builder = apply {
            this.country = country?.uppercase()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) // For paymentsheet
        fun setCountryCode(countryCode: CountryCode?): Builder = apply {
            this.country = countryCode?.value
        }

        fun setLine1(line1: String?): Builder = apply {
            this.line1 = line1
        }

        fun setLine2(line2: String?): Builder = apply {
            this.line2 = line2
        }

        fun setPostalCode(postalCode: String?): Builder = apply {
            this.postalCode = postalCode
        }

        fun setState(state: String?): Builder = apply {
            this.state = state
        }

        fun build(): Address {
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
        private const val PARAM_CITY = "city"

        // 2 Character Country Code
        private const val PARAM_COUNTRY = "country"
        private const val PARAM_LINE_1 = "line1"
        private const val PARAM_LINE_2 = "line2"
        private const val PARAM_POSTAL_CODE = "postal_code"
        private const val PARAM_STATE = "state"

        @JvmStatic
        fun fromJson(jsonObject: JSONObject?): Address? {
            return jsonObject?.let {
                AddressJsonParser().parse(it)
            }
        }
    }
}
