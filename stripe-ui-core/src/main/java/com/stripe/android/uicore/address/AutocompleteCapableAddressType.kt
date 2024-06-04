package com.stripe.android.uicore.address

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import com.stripe.android.uicore.elements.IsPlacesAvailable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AutocompleteCapableAddressType {
    val googleApiKey: String?
    val autocompleteCountries: Set<String>?
    val onNavigation: () -> Unit

    fun supportsAutoComplete(
        country: String?,
        isPlacesAvailable: IsPlacesAvailable,
    ): Boolean {
        val supportedCountries = autocompleteCountries
        val autocompleteSupportsCountry = supportedCountries
            ?.map { it.toLowerCase(Locale.current) }
            ?.contains(country?.toLowerCase(Locale.current)) == true
        val autocompleteAvailable = isPlacesAvailable() &&
            !googleApiKey.isNullOrBlank()
        return autocompleteSupportsCountry && autocompleteAvailable
    }
}
