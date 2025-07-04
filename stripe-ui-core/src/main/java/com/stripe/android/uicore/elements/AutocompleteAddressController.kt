package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressController(
    val identifier: IdentifierSpec,
    val initialValues: Map<IdentifierSpec, String?>,
    interactorFactory: AutocompleteAddressInteractor.Factory,
    countryCodes: Set<String> = emptySet(),
    private val countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        initialValues[IdentifierSpec.Country]
    ),
    private val phoneNumberState: PhoneNumberState,
    private val sameAsShippingElement: SameAsShippingElement?,
    private val shippingValuesMap: Map<IdentifierSpec, String?>?,
    private val isPlacesAvailable: IsPlacesAvailable = DefaultIsPlacesAvailable(),
    private val hideCountry: Boolean = false,
    private val hideName: Boolean = true,
) : SectionFieldErrorController, SectionFieldComposable {
    private val interactor = interactorFactory.create()

    private val config = interactor.autocompleteConfig

    private var expandForm = false

    override val error: StateFlow<FieldError?> = stateFlowOf(null)

    val addressElementFlow = MutableStateFlow(
        createAddressElement(initialValues, toAddressInputMode(expandForm, initialValues))
    )

    val formFieldValues = addressElementFlow.flatMapLatestAsStateFlow { addressElement ->
        addressElement.getFormFieldValueFlow()
    }

    val textFieldIdentifiers = addressElementFlow.flatMapLatestAsStateFlow { addressElement ->
        addressElement.getTextFieldIdentifiers()
    }

    init {
        interactor.register { event ->
            val currentValues = getCurrentValues()
            val newValues = event.values ?: currentValues

            when (event) {
                is AutocompleteAddressInteractor.Event.OnValues -> Unit
                is AutocompleteAddressInteractor.Event.OnExpandForm -> expandForm = true
            }

            val newAddressInputMode = toAddressInputMode(expandForm, newValues)

            if (currentValues != newValues || newAddressInputMode != addressElementFlow.value.addressInputMode) {
                newValues[IdentifierSpec.Country]?.let {
                    countryDropdownFieldController.onRawValueChange(it)
                }

                addressElementFlow.value =
                    createAddressElement(newValues, toAddressInputMode(expandForm, newValues))
            }
        }
    }

    private fun createAddressElement(
        values: Map<IdentifierSpec, String?>,
        addressInputMode: AddressInputMode,
    ): AddressElement {
        return AddressElement(
            _identifier = identifier,
            rawValuesMap = values,
            addressInputMode = addressInputMode,
            countryDropdownFieldController = countryDropdownFieldController,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            isPlacesAvailable = isPlacesAvailable,
            hideCountry = hideCountry,
            hideName = hideName,
        )
    }

    private fun toAddressInputMode(
        expandForm: Boolean,
        values: Map<IdentifierSpec, String?>
    ): AddressInputMode {
        val googlePlacesApiKey = config.googlePlacesApiKey

        return if (googlePlacesApiKey == null) {
            AddressInputMode.NoAutocomplete(phoneNumberState)
        } else if (expandForm || values[IdentifierSpec.Line1] != null) {
            AddressInputMode.AutocompleteExpanded(
                googleApiKey = googlePlacesApiKey,
                autocompleteCountries = config.autocompleteCountries,
                phoneNumberState = phoneNumberState,
                onNavigation = {
                    interactor.onAutocomplete(
                        country = countryDropdownFieldController.rawFieldValue.value ?: ""
                    )
                },
            )
        } else {
            AddressInputMode.AutocompleteCondensed(
                googleApiKey = googlePlacesApiKey,
                autocompleteCountries = config.autocompleteCountries,
                phoneNumberState = phoneNumberState,
                onNavigation = {
                    interactor.onAutocomplete(
                        country = countryDropdownFieldController.rawFieldValue.value ?: ""
                    )
                },
            )
        }
    }

    private fun getCurrentValues() = formFieldValues.value.toMap().mapValues {
        it.value.value
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
