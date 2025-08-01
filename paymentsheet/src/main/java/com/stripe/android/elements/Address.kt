package com.stripe.android.elements

import android.os.Parcelable
import dev.drewhamilton.poko.Poko
import kotlinx.parcelize.Parcelize

@Parcelize
@Poko
class Address(
    /**
     * City, district, suburb, town, or village.
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val city: String? = null,
    /**
     * Two-letter country code (ISO 3166-1 alpha-2).
     */
    val country: String? = null,
    /**
     * Address line 1 (e.g., street, PO Box, or company name).
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val line1: String? = null,
    /**
     * Address line 2 (e.g., apartment, suite, unit, or building).
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val line2: String? = null,
    /**
     * ZIP or postal code.
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val postalCode: String? = null,
    /**
     * State, county, province, or region.
     * The value set is displayed in the payment sheet as-is.
     * Depending on the payment method, the customer may be required to edit this value.
     */
    val state: String? = null
) : Parcelable {
    /**
     * [Address] builder for cleaner object creation from Java.
     */
    class Builder {
        private var city: String? = null
        private var country: String? = null
        private var line1: String? = null
        private var line2: String? = null
        private var postalCode: String? = null
        private var state: String? = null

        fun city(city: String?) = apply { this.city = city }
        fun country(country: String?) = apply { this.country = country }
        fun line1(line1: String?) = apply { this.line1 = line1 }
        fun line2(line2: String?) = apply { this.line2 = line2 }
        fun postalCode(postalCode: String?) = apply { this.postalCode = postalCode }
        fun state(state: String?) = apply { this.state = state }

        fun build() = Address(city, country, line1, line2, postalCode, state)
    }
}
