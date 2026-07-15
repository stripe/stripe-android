package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview

/**
 * Static lookup for the minimum billing address fields, beyond the country picker, that a
 * customer must provide for a Checkout Session using automatic tax with the billing address
 * as the tax source (tax status [CheckoutSession.Tax.Status.RequiresBillingAddress]).
 *
 * Most countries only need the country. The countries in [additionalFieldsByCountry] need more.
 */
@CheckoutSessionPreview
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CheckoutBillingAddressRequirements {

    /**
     * A billing address field required in addition to the country.
     */
    @CheckoutSessionPreview
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    enum class Field {
        LINE1,
        CITY,
        STATE,
        POSTAL_CODE,
    }

    private val additionalFieldsByCountry: Map<String, List<Field>> = mapOf(
        "CA" to listOf(Field.POSTAL_CODE),
        "GB" to listOf(Field.POSTAL_CODE),
        "IN" to listOf(Field.POSTAL_CODE),
        "PR" to listOf(Field.LINE1, Field.CITY, Field.POSTAL_CODE),
        "US" to listOf(Field.LINE1, Field.CITY, Field.STATE, Field.POSTAL_CODE),
    )

    /**
     * Returns the billing address fields required in addition to the country for [countryCode].
     * Returns an empty list when only the country is required.
     *
     * @param countryCode a two-letter ISO 3166-1 alpha-2 country code (case insensitive).
     */
    fun requiredFields(countryCode: String): List<Field> {
        return additionalFieldsByCountry[countryCode.uppercase()].orEmpty()
    }
}
