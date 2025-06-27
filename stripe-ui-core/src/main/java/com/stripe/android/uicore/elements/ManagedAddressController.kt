package com.stripe.android.uicore.elements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.mapAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

class ManagedAddressController(
    val identifier: IdentifierSpec,
    rawValuesMap: Map<IdentifierSpec, String?> = emptyMap(),
    managedAddressManagerFactory: ManagedAddressManager.Factory?,
    countryCodes: Set<String> = emptySet(),
    countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        rawValuesMap[IdentifierSpec.Country]
    ),
    sameAsShippingElement: SameAsShippingElement?,
    shippingValuesMap: Map<IdentifierSpec, String?>?,
    private val isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable(),
    private val hideCountry: Boolean = false,
    private val phoneNumberState: PhoneNumberState,
) : SectionFieldErrorController, SectionFieldComposable {
    private val autocompleteManager = managedAddressManagerFactory?.create(rawValuesMap)

    private val addressType = autocompleteManager?.run {
        state.mapAsStateFlow { state ->
            when (state) {
                is ManagedAddressManager.State.Condensed -> AddressType.ShippingCondensed(
                    googleApiKey = googlePlacesApiKey,
                    autocompleteCountries = countryCodes,
                    phoneNumberState = phoneNumberState,
                    onNavigation = {
                        navigateToAutocomplete(countryDropdownFieldController.rawFieldValue.value ?: "")
                    }
                )
                is ManagedAddressManager.State.Expanded -> AddressType.ShippingExpanded(
                    googleApiKey = googlePlacesApiKey,
                    autocompleteCountries = countryCodes,
                    phoneNumberState = phoneNumberState,
                    onNavigation = {
                        navigateToAutocomplete(countryDropdownFieldController.rawFieldValue.value ?: "")
                    }
                )
            }
        }
    } ?: MutableStateFlow(AddressType.Normal())

    private val initialValues = autocompleteManager?.run {
        state.mapAsStateFlow { state ->
            when (state) {
                is ManagedAddressManager.State.Condensed -> rawValuesMap
                is ManagedAddressManager.State.Expanded -> state.values ?: rawValuesMap
            }
        }
    } ?: MutableStateFlow(rawValuesMap)

    override val error: StateFlow<FieldError?> = stateFlowOf(null)

    val addressElementFlow = combineAsStateFlow(
        addressType,
        initialValues
    ) { addressType, initialValues ->
        AddressElement(
            _identifier = identifier,
            rawValuesMap = initialValues,
            addressType = addressType,
            countryDropdownFieldController = countryDropdownFieldController,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            isPlacesAvailable = isPlacesAvailable,
            hideCountry = hideCountry,
        )
    }

    @Composable
    override fun ComposeUI(
        enabled: Boolean,
        field: SectionFieldElement,
        modifier: Modifier,
        hiddenIdentifiers: Set<IdentifierSpec>,
        lastTextFieldIdentifier: IdentifierSpec?
    ) {
        val element by addressElementFlow.collectAsState()

        AddressElementUI(
            enabled = enabled,
            controller = element.controller,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            modifier = modifier,
        )
    }
}
