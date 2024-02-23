package com.stripe.android.uicore.elements

import android.os.Parcelable
import androidx.annotation.RestrictTo
import com.stripe.android.uicore.address.AutocompleteCapableAddressType
import kotlinx.parcelize.Parcelize

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
sealed class AddressType : Parcelable {
    abstract val phoneNumberState: PhoneNumberState

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class ShippingCondensed(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberState: PhoneNumberState,
        override val onNavigation: () -> Unit
    ) : AddressType(), AutocompleteCapableAddressType

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class ShippingExpanded(
        override val googleApiKey: String?,
        override val autocompleteCountries: Set<String>?,
        override val phoneNumberState: PhoneNumberState,
        override val onNavigation: () -> Unit,
    ) : AddressType(), AutocompleteCapableAddressType

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Parcelize
    data class Normal(
        override val phoneNumberState: PhoneNumberState =
            PhoneNumberState.HIDDEN
    ) : AddressType()
}
