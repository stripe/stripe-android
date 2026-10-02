package com.stripe.android.uicore.elements

import androidx.annotation.RestrictTo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.stripe.android.uicore.R
import com.stripe.android.uicore.utils.collectAsState
import com.stripe.android.uicore.utils.combineAsStateFlow
import com.stripe.android.uicore.utils.flatMapLatestAsStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AutocompleteAddressController(
    val identifier: FormFieldId,
    val initialValues: Map<FormFieldId, String?>,
    interactorFactory: AutocompleteAddressInteractor.Factory,
    countryCodes: Set<String> = emptySet(),
    private val countryDropdownFieldController: DropdownFieldController = DropdownFieldController(
        CountryConfig(countryCodes),
        initialValues[FormFieldId.Country]
    ),
    private val phoneNumberConfig: AddressFieldConfiguration,
    private val nameConfig: AddressFieldConfiguration,
    private val emailConfig: AddressFieldConfiguration,
    private val sameAsShippingElement: SameAsShippingElement?,
    private val shippingValuesMap: Map<FormFieldId, String?>?,
    private val hideCountry: Boolean = false,
) : SectionFieldValidationController, SectionFieldComposable {
    private val interactor = interactorFactory.create()

    private val config = interactor.autocompleteConfig

    private val inlineAutocompleteActive =
        config.isInlineAutocompleteEnabled && (
            config.shouldUseStripeHostedAutocomplete ||
                (!config.googlePlacesApiKey.isNullOrBlank() && config.isPlacesAvailable)
            )

    private val inlineAutocompleteHandler: InlineAutocompleteHandler? =
        if (inlineAutocompleteActive) {
            object : InlineAutocompleteHandler {
                override val predictionsState = interactor.inlinePredictionsState

                override fun onPredictionSelected(predictionId: String) {
                    interactor.onPredictionSelected(predictionId)
                }

                override fun onDismissed() {
                    interactor.onDismissed()
                }

                override fun onFocusLost() {
                    interactor.onFocusLost()
                }

                override fun onFocusGained() {
                    interactor.onFocusGained()
                }

                override fun onEnterManually() {
                    interactor.onEnterManuallyFromInline()
                }

                override fun getAttributionDrawable(isDarkTheme: Boolean): Int? {
                    return R.drawable.stripe_google_maps_logo
                }

                override fun onSearchActivated() {
                    interactor.onSearchActivated()
                }
            }
        } else {
            null
        }

    private var expandForm = false

    private val isValidating = MutableStateFlow(false)

    val countryElement = CountryElement(
        FormFieldId.Country,
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

    private val inlineQuery: StateFlow<String> = addressElementFlow.flatMapLatestAsStateFlow {
        it.inlineQuery
    }

    override val validationMessage: StateFlow<FieldValidationMessage?> = addressController.flatMapLatestAsStateFlow {
        it.validationMessage
    }

    init {
        if (inlineAutocompleteActive) {
            interactor.observeQueryChanges(inlineQuery, countryDropdownFieldController.rawFieldValue)
        }

        interactor.register { event ->
            val currentValues = getCurrentValues()

            /*
             * Merges the current and new values together. New value keys will override current
             * value keys if provided.
             */
            val newValues = currentValues.plus(event.values ?: emptyMap())

            when (event) {
                is AutocompleteAddressInteractor.Event.OnValues -> Unit
                is AutocompleteAddressInteractor.Event.OnExpandForm -> expandForm = true
            }

            val newAddressInputMode = toAddressInputMode(expandForm, newValues)

            if (currentValues != newValues || newAddressInputMode != addressElementFlow.value.addressInputMode) {
                newValues[FormFieldId.Country]?.let {
                    countryDropdownFieldController.onRawValueChange(it)
                }

                _addressElementFlow.value =
                    createAddressElement(newValues, newAddressInputMode)
            }
        }
    }

    fun setRawValue(values: Map<FormFieldId, String?>) {
        _addressElementFlow.value = createAddressElement(values, toAddressInputMode(expandForm, values))
    }

    private fun createAddressElement(
        values: Map<FormFieldId, String?>,
        addressInputMode: AddressInputMode,
    ): AddressElement {
        val handler = if (isCountrySupported(values[FormFieldId.Country])) {
            inlineAutocompleteHandler
        } else {
            null
        }
        return AddressElement(
            _identifier = identifier,
            rawValuesMap = values,
            addressInputMode = addressInputMode,
            countryElement = countryElement,
            sameAsShippingElement = sameAsShippingElement,
            shippingValuesMap = shippingValuesMap,
            isPlacesAvailable = config.isPlacesAvailable,
            hideCountry = hideCountry,
            inlineAutocompleteHandler = handler,
        )
    }

    private fun isCountrySupported(country: String?): Boolean {
        if (country.isNullOrBlank()) return true
        val supported = config.autocompleteCountries
        return supported.isEmpty() || supported.any { it.equals(country, ignoreCase = true) }
    }

    private fun toAddressInputMode(
        expandForm: Boolean,
        values: Map<FormFieldId, String?>
    ): AddressInputMode {
        val googlePlacesApiKey = config.googlePlacesApiKey
        val countrySupported = isCountrySupported(values[FormFieldId.Country])

        val showInline = inlineAutocompleteActive && !expandForm &&
            countrySupported && values[FormFieldId.Line1].isNullOrEmpty()
        return if (showInline) {
            AddressInputMode.AutocompleteInline(
                googleApiKey = googlePlacesApiKey,
                autocompleteCountries = config.autocompleteCountries,
                phoneNumberConfig = phoneNumberConfig,
                nameConfig = nameConfig,
                emailConfig = emailConfig,
            )
        } else if (config.isInlineAutocompleteEnabled || googlePlacesApiKey == null) {
            AddressInputMode.NoAutocomplete(
                phoneNumberConfig = phoneNumberConfig,
                nameConfig = nameConfig,
                emailConfig = emailConfig,
            )
        } else if (expandForm || values[FormFieldId.Line1] != null) {
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
        hiddenIdentifiers: Set<FormFieldId>,
        lastTextFieldIdentifier: FormFieldId?
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
