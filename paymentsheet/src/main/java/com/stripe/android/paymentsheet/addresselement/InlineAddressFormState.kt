package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.RestrictTo
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.utils.mapAsStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the input of an [InlineAddressForm]. The input is not saved across configuration changes
 * or process death, so hold this in a `ViewModel`.
 *
 * @param initialAddress the address to prefill the fields with.
 * @param allowedCountries the countries to offer, or every country if empty. An [initialAddress]
 *   in another country doesn't prefill the fields.
 * @param nameField whether to show the name field. A hidden name keeps the one in
 *   [initialAddress] in [enteredAddress] and [completeAddress].
 * @param phone whether to show the phone number field, and whether it is required.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InlineAddressFormState(
    initialAddress: AddressDetails?,
    allowedCountries: Set<String>,
    nameField: AddressLauncher.NameField,
    phone: AddressLauncher.AdditionalFieldsConfiguration.FieldConfiguration,
) {
    private val prefilledAddress = initialAddress?.takeIfCountryAllowed(allowedCountries)

    internal val formController = AddressFormController(
        initialValues = prefilledAddress?.toIdentifierMap() ?: emptyMap(),
        config = AddressLauncher.Configuration(
            allowedCountries = allowedCountries,
            additionalFields = AddressLauncher.AdditionalFieldsConfiguration(phone = phone),
            billingAddress = null,
            nameField = nameField,
        ),
        interactor = NoAutocompleteAddressInteractor,
    )

    // The customer can't edit a hidden name, so the address keeps the initial one.
    private val hiddenName = initialAddress?.name.takeIf {
        nameField == AddressLauncher.NameField.Never
    }

    /**
     * The address entered so far, including incomplete and invalid fields.
     */
    val enteredAddress: StateFlow<AddressDetails> =
        formController.uncompletedFormValues.mapAsStateFlow { formValues ->
            formValues.toAddressDetails(
                hiddenName = hiddenName,
                isCheckboxSelected = null,
            )
        }

    /**
     * The entered address once every required field is complete, or null otherwise.
     */
    val completeAddress: StateFlow<AddressDetails?> =
        formController.completeFormValues.mapAsStateFlow { formValues ->
            formValues?.toAddressDetails(
                hiddenName = hiddenName,
                isCheckboxSelected = null,
            )
        }

    /**
     * Whether the entered values differ from [initialAddress].
     */
    fun hasChanges(): Boolean = formController.hasChanges()

    /**
     * Shows an error on every field that is incomplete or invalid.
     */
    fun validate() {
        formController.elements.forEach { element ->
            element.onValidationStateChanged(true)
        }
    }
}

private object NoAutocompleteAddressInteractor : AutocompleteAddressInteractor {
    override val autocompleteConfig = AutocompleteAddressInteractor.Config(
        googlePlacesApiKey = null,
        autocompleteCountries = emptySet(),
    )

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) = Unit

    override fun onAutocomplete(country: String) = Unit
}
