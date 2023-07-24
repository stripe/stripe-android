package com.stripe.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AddressJapanParams(
    /**
     * City or ward.
     */
    val city: String? = null,

    /**
     * Two-letter country code (ISO 3166-1 alpha-2).
     */
    val country: String? = null,

    /**
     * Block or building number.
     */
    val line1: String? = null,

    /**
     * Building details.
     */
    val line2: String? = null,

    /**
     * Postal code.
     */
    val postalCode: String? = null,

    /**
     * Prefecture.
     */
    val state: String? = null,

    /**
     * Town or cho-me.
     */
    val town: String? = null
) : StripeParamsModel, Parcelable {
    override fun toParamMap(): Map<String, Any> {
        return listOf(
            PARAM_CITY to city,
            PARAM_COUNTRY to country,
            PARAM_LINE_1 to line1,
            PARAM_LINE_2 to line2,
            PARAM_POSTAL_CODE to postalCode,
            PARAM_STATE to state,
            PARAM_TOWN to town
        ).fold(emptyMap()) { acc, (key, value) ->
            acc.plus(
                value?.let { mapOf(key to it) }.orEmpty()
            )
        }
    }

    class Builder {
        private var city: String? = null
        private var country: String? = null
        private var line1: String? = null
        private var line2: String? = null
        private var postalCode: String? = null
        private var state: String? = null
        private var town: String? = null

        /**
         * @param city City or ward.
         */
        fun setCity(city: String?): Builder = apply {
            this.city = city
        }

        /**
         * @param country Two-letter country code (ISO 3166-1 alpha-2).
         */
        fun setCountry(country: String?): Builder = apply {
            this.country = country?.uppercase()
        }

        /**
         * @param line1 Block or building number.
         */
        fun setLine1(line1: String?): Builder = apply {
            this.line1 = line1
        }

        /**
         * @param line2 Building details.
         */
        fun setLine2(line2: String?): Builder = apply {
            this.line2 = line2
        }

        /**
         * @param postalCode Postal code.
         */
        fun setPostalCode(postalCode: String?): Builder = apply {
            this.postalCode = postalCode
        }

        /**
         * @param state Prefecture.
         */
        fun setState(state: String?): Builder = apply {
            this.state = state
        }

        /**
         * @param town Town or cho-me.
         */
        fun setTown(town: String?): Builder = apply {
            this.town = town
        }

        fun build(): AddressJapanParams {
            return AddressJapanParams(
                city = city,
                country = country,
                line1 = line1,
                line2 = line2,
                postalCode = postalCode,
                state = state,
                town = town
            )
        }
    }

    private companion object {
        private const val PARAM_CITY = "city"
        private const val PARAM_COUNTRY = "country"
        private const val PARAM_LINE_1 = "line1"
        private const val PARAM_LINE_2 = "line2"
        private const val PARAM_POSTAL_CODE = "postal_code"
        private const val PARAM_STATE = "state"
        private const val PARAM_TOWN = "town"
    }
}
