package com.stripe.android.checkout

import androidx.annotation.RestrictTo
import com.stripe.android.paymentelement.CheckoutSessionPreview
import com.stripe.android.uicore.elements.IdentifierSpec

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

    private val additionalFieldsByCountry: Map<String, List<IdentifierSpec>> = mapOf(
        "CA" to listOf(IdentifierSpec.PostalCode),
        "GB" to listOf(IdentifierSpec.PostalCode),
        "IN" to listOf(IdentifierSpec.PostalCode),
        "PR" to listOf(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.PostalCode),
        "US" to listOf(IdentifierSpec.Line1, IdentifierSpec.City, IdentifierSpec.State, IdentifierSpec.PostalCode),
    )

    /**
     * Returns the billing address fields required in addition to the country for [countryCode].
     * Returns an empty list when only the country is required.
     *
     * @param countryCode a two-letter ISO 3166-1 alpha-2 country code (case insensitive).
     */
    fun requiredFields(countryCode: String): List<IdentifierSpec> {
        return additionalFieldsByCountry[countryCode.uppercase()].orEmpty()
    }
}
