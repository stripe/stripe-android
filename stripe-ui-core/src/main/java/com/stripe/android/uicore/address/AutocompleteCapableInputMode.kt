package com.stripe.android.uicore.address

import androidx.annotation.RestrictTo
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AutocompleteCapableInputMode {
    val googleApiKey: String?
    val autocompleteCountries: Set<String>?
    val onNavigation: () -> Unit

    fun supportsAutoComplete(
        country: String?,
        isPlacesAvailable: Boolean,
    ): Boolean {
        val supportedCountries = autocompleteCountries
        val autocompleteSupportsCountry = supportedCountries
            ?.map { it.toLowerCase(Locale.current) }
            ?.contains(country?.toLowerCase(Locale.current)) == true
        val autocompleteAvailable = isPlacesAvailable &&
            !googleApiKey.isNullOrBlank()
        return autocompleteSupportsCountry && autocompleteAvailable
    }
}
