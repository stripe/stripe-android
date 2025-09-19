package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
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
    private val phoneNumberConfig: AddressFieldConfiguration,
    private val nameConfig: AddressFieldConfiguration,
    private val emailConfig: AddressFieldConfiguration,
    private val sameAsShippingElement: SameAsShippingElement?,
    private val shippingValuesMap: Map<IdentifierSpec, String?>?,
    private val hideCountry: Boolean = false,
) : SectionFieldErrorController, SectionFieldComposable {
    private val interactor = interactorFactory.create()

    private val config = interactor.autocompleteConfig

    private var expandForm = false

    private val isValidating = MutableStateFlow(false)

    val countryElement = CountryElement(
        IdentifierSpec.Country,
        countryDropdownFieldController,
    )

    private val _addressElementFlow = MutableStateFlow(
        createAddressElement(initialValues, toAddressInputMode(expandForm, initialValues))
    )

    val addressElementFlow = combineAsStateFlow(
        _addressElementFlow,
        isValidating,
    ) { element, isValidating ->
        element.apply {
            onValidationStateChanged(isValidating)
        }
    }

    val formFieldValues = addressElementFlow.flatMapLatestAsStateFlow { addressElement ->
        addressElement.getFormFieldValueFlow()
    }

    val textFieldIdentifiers = addressElementFlow.flatMapLatestAsStateFlow { addressElement ->
        addressElement.getTextFieldIdentifiers()
    }

    val addressController = addressElementFlow.flatMapLatestAsStateFlow {
        it.addressController
    }

    override val error: StateFlow<FieldError?> = addressController.flatMapLatestAsStateFlow {
        it.error
    }

    init {
        interactor.register { event ->
            val currentValues = getCurrentValues()
            val newValues = currentValues.plus(event.values ?: emptyMap())

            when (event) {
                is AutocompleteAddressInteractor.Event.OnValues -> Unit
                is AutocompleteAddressInteractor.Event.OnExpandForm -> expandForm = true
            }

            val newAddressInputMode = toAddressInputMode(expandForm, newValues)

            if (currentValues != newValues || newAddressInputMode != addressElementFlow.value.addressInputMode) {
                newValues[IdentifierSpec.Country]?.let {
                    countryDropdownFieldController.onRawValueChange(it)
                }

                _addressElementFlow.value =
                    createAddressElement(newValues, toAddressInputMode(expandForm, newValues))
            }
        }
    }

    fun setRawValue(values: Map<IdentifierSpec, String?>) {
        _addressElementFlow.value = createAddressElement(values, toAddressInputMode(expandForm, values))
    }

    private fun createAddressElement(
        values: Map<IdentifierSpec, String?>,
        addressInputMode: AddressInputMode,
    ): AddressElement {
        return AddressElement(
            _identifier = identifier,
            rawValuesMap = values,
            addressInputMode = addressInputMode,
            countryElement = countryElement,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            isPlacesAvailable = config.isPlacesAvailable,
            hideCountry = hideCountry,
        )
    }

    private fun toAddressInputMode(
        expandForm: Boolean,
        values: Map<IdentifierSpec, String?>
    ): AddressInputMode {
        val googlePlacesApiKey = config.googlePlacesApiKey

        return if (googlePlacesApiKey == null) {
            AddressInputMode.NoAutocomplete(
                phoneNumberConfig = phoneNumberConfig,
                nameConfig = nameConfig,
                emailConfig = emailConfig,
            )
        } else if (expandForm || values[IdentifierSpec.Line1] != null) {
            AddressInputMode.AutocompleteExpanded(
                googleApiKey = googlePlacesApiKey,
                autocompleteCountries = config.autocompleteCountries,
                phoneNumberConfig = phoneNumberConfig,
                nameConfig = nameConfig,
                emailConfig = emailConfig,
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
                phoneNumberConfig = phoneNumberConfig,
                nameConfig = nameConfig,
                emailConfig = emailConfig,
                onNavigation = {
                    interactor.onAutocomplete(
                        country = countryDropdownFieldController.rawFieldValue.value ?: ""
                    )
                },
            )
        }
    }

    override fun onValidationStateChanged(isValidating: Boolean) {
        this.isValidating.value = isValidating
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
        val controller by addressController.collectAsState()

        AddressElementUI(
            enabled = enabled,
            controller = controller,
            hiddenIdentifiers = hiddenIdentifiers,
            lastTextFieldIdentifier = lastTextFieldIdentifier,
            modifier = modifier,
        )
    }
}
