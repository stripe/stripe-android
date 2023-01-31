package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.address.AutocompleteCapableAddressType

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class AddressType {
    abstract val phoneNumberState: PhoneNumberState

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class ShippingCondensed(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberState: PhoneNumberState,
        override val onNavigation: () -> Unit
    ) : AddressType(), AutocompleteCapableAddressType

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class ShippingExpanded constructor(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberState: PhoneNumberState,
        override val onNavigation: () -> Unit,
    ) : AddressType(), AutocompleteCapableAddressType

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    data class Normal(
        override val phoneNumberState: PhoneNumberState =
            PhoneNumberState.HIDDEN
    ) : AddressType()
}
