package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.stateFlowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressController(
    val identifier: IdentifierSpec,
    val initialValues: Map<IdentifierSpec, String?>,
    private val interactor: AutocompleteAddressInteractor,
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
    private val config = interactor.autocompleteConfig

    private var currentValues = initialValues
    private var expandForm = false
    private val addressInputMode = MutableStateFlow(toAddressInputMode(expandForm, initialValues))

    override val error: StateFlow<FieldError?> = stateFlowOf(null)

    val addressElementFlow = addressInputMode.map {
        createAddressElement(currentValues, it)
    }.stateIn(
        scope = interactor.interactorScope,
        started = SharingStarted.Lazily,
        initialValue = createAddressElement(currentValues, addressInputMode.value),
    )

    val formFieldValues = addressElementFlow.flatMapLatest { addressElement ->
        addressElement.getFormFieldValueFlow()
    }.stateIn(
        scope = interactor.interactorScope,
        started = SharingStarted.Lazily,
        initialValue = addressElementFlow.value.getFormFieldValueFlow().value,
    )

    val textFieldIdentifiers = addressElementFlow.flatMapLatest { addressElement ->
        addressElement.getTextFieldIdentifiers()
    }.stateIn(
        scope = interactor.interactorScope,
        started = SharingStarted.Lazily,
        initialValue = addressElementFlow.value.getTextFieldIdentifiers().value,
    )

    init {
        interactor.interactorScope.launch {
            interactor.autocompleteEvent.collectLatest { event ->
                currentValues = event.values ?: currentValues

                when (event) {
                    is AutocompleteAddressInteractor.Event.OnValues -> Unit
                    is AutocompleteAddressInteractor.Event.OnExpandForm -> expandForm = true
                }

                val newAddressInputMode = toAddressInputMode(expandForm, currentValues)

                addressInputMode.value = newAddressInputMode
            }
        }

        interactor.interactorScope.launch {
            formFieldValues.collectLatest { values ->
                currentValues = values.toMap().mapValues {
                    it.value.value
                }
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
