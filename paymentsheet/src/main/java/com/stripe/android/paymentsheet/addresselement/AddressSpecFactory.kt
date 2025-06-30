package com.stripe.android.paymentsheet.addresselement

import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.uicore.elements.AddressInputMode
import com.stripe.android.uicore.elements.PhoneNumberState

internal object AddressSpecFactory {
    fun create(
        condensedForm: Boolean,
        config: AddressLauncher.Configuration?,
        onNavigation: () -> Unit
    ): AddressSpec {
        val phoneNumberState = parsePhoneNumberConfig(config?.additionalFields?.phone)
        val addressSpec = if (condensedForm) {
            AddressSpec(
                showLabel = false,
                type = AddressInputMode.AutocompleteCondensed(
                    googleApiKey = config?.googlePlacesApiKey,
                    autocompleteCountries = config?.autocompleteCountries,
                    phoneNumberState = phoneNumberState,
                    onNavigation = onNavigation,
                )
            )
        } else {
            AddressSpec(
                showLabel = false,
                type = AddressInputMode.AutocompleteExpanded(
                    googleApiKey = config?.googlePlacesApiKey,
                    autocompleteCountries = config?.autocompleteCountries,
                    phoneNumberState = phoneNumberState,
                    onNavigation = onNavigation,
                )
            )
        }

        val addressSpecWithAllowedCountries = config?.allowedCountries?.run {
            addressSpec.copy(allowedCountryCodes = this)
        }

        return addressSpecWithAllowedCountries ?: addressSpec
    }

    // This mapping is required to prevent merchants from depending on ui-core.
    fun parsePhoneNumberConfig(
        configuration: AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration?
    ): PhoneNumberState {
        return when (configuration) {
            AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.HIDDEN ->
                PhoneNumberState.HIDDEN
            AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.OPTIONAL ->
                PhoneNumberState.OPTIONAL
            AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration.REQUIRED ->
                PhoneNumberState.REQUIRED
            null -> PhoneNumberState.OPTIONAL
        }
    }
}
