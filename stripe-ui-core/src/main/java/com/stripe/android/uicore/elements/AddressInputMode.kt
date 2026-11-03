package com.stripe.android.uicore.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class AddressInputMode : Parcelable {
    abstract val phoneNumberConfig: AddressFieldConfiguration
    abstract val nameConfig: AddressFieldConfiguration
    abstract val emailConfig: AddressFieldConfiguration

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class AutocompleteInline(
        val googleApiKey: String?,
        val autocompleteCountries: Set<String>?,
        override val phoneNumberConfig: AddressFieldConfiguration,
        override val nameConfig: AddressFieldConfiguration,
        override val emailConfig: AddressFieldConfiguration,
    ) : AddressInputMode()

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class NoAutocomplete(
        override val phoneNumberConfig: AddressFieldConfiguration =
            AddressFieldConfiguration.HIDDEN,
        override val nameConfig: AddressFieldConfiguration =
            AddressFieldConfiguration.HIDDEN,
        override val emailConfig: AddressFieldConfiguration =
            AddressFieldConfiguration.HIDDEN,
    ) : AddressInputMode()
}
