package com.stripe.android.uicore.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.address.AutocompleteCapableInputMode
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class AddressInputMode : Parcelable {
    abstract val phoneNumberState: PhoneNumberState

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class AutocompleteCondensed(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberState: PhoneNumberState,
        override val onNavigation: () -> Unit
    ) : AddressInputMode(), AutocompleteCapableInputMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class AutocompleteExpanded(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberState: PhoneNumberState,
        override val onNavigation: () -> Unit,
    ) : AddressInputMode(), AutocompleteCapableInputMode

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class NoAutocomplete(
        override val phoneNumberState: PhoneNumberState =
            PhoneNumberState.HIDDEN
    ) : AddressInputMode()
}
