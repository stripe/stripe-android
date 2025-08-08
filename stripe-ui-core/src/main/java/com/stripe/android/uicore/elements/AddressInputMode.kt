package com.stripe.android.uicore.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.address.AutocompleteCapableInputMode
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class AddressInputMode : Parcelable {
    abstract val phoneNumberConfig: AddressFieldConfiguration
    abstract val nameConfig: AddressFieldConfiguration

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class AutocompleteCondensed(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberConfig: AddressFieldConfiguration,
        override val nameConfig: AddressFieldConfiguration,
        override val onNavigation: () -> Unit
    ) : AddressInputMode(), AutocompleteCapableInputMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class AutocompleteExpanded(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberConfig: AddressFieldConfiguration,
        override val nameConfig: AddressFieldConfiguration,
        override val onNavigation: () -> Unit,
    ) : AddressInputMode(), AutocompleteCapableInputMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class NoAutocomplete(
        override val phoneNumberConfig: AddressFieldConfiguration =
            AddressFieldConfiguration.HIDDEN,
        override val nameConfig: AddressFieldConfiguration =
            AddressFieldConfiguration.HIDDEN,
    ) : AddressInputMode()
}
