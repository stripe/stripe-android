package com.stripe.android.ui.core.elements

import androidx.annotation.RestrictTo
import com.stripe.android.core.model.CountryUtils
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the specification for a country field.
 * @property allowedCountryCodes: a list of country code that should be shown.  If empty all
 * countries will be shown.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Serializable
@Parcelize
data class CountrySpec(
    @SerialName("allowed_country_codes")
    val allowedCountryCodes: Set<String> = CountryUtils.supportedBillingCountries
) : FormItemSpec()
