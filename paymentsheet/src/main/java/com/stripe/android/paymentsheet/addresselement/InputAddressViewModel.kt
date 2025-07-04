package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class InputAddressViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    private val eventReporter: AddressLauncherEventReporter,
) : ViewModel(), AutocompleteAddressInteractor {
    private var eventListener: ((AutocompleteAddressInteractor.Event) -> Unit)? = null

    private val _collectedAddress = MutableStateFlow(args.config?.address)
    val collectedAddress: StateFlow<AddressDetails?> = _collectedAddress

    override val autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
        googlePlacesApiKey = args.config?.googlePlacesApiKey,
        autocompleteCountries = args.config?.autocompleteCountries ?: emptySet(),
    )

    val addressFormController = AddressFormController(
        interactor = this,
        config = args.config,
    )

    private val _formEnabled = MutableStateFlow(true)
    val formEnabled: StateFlow<Boolean> = _formEnabled

    private val _checkboxChecked = MutableStateFlow(false)
    val checkboxChecked: StateFlow<Boolean> = _checkboxChecked

    init {
        viewModelScope.launch {
            navigator.getResultFlow<AddressElementNavigator.AutocompleteEvent?>(
                AddressElementNavigator.AutocompleteEvent.KEY
            )?.collect { event ->
                val oldAddress = _collectedAddress.value
                val newAddress = event?.addressDetails
                val autocompleteAddress = AddressDetails(
                    name = oldAddress?.name ?: newAddress?.name,
                    address = newAddress?.address ?: oldAddress?.address,
                    phoneNumber = oldAddress?.phoneNumber ?: newAddress?.phoneNumber,
                    isCheckboxSelected = oldAddress?.isCheckboxSelected
                        ?: newAddress?.isCheckboxSelected
                )

                val values = autocompleteAddress.toIdentifierMap()

                when (event) {
                    is AddressElementNavigator.AutocompleteEvent.OnEnterManually -> {
                        eventListener?.invoke(AutocompleteAddressInteractor.Event.OnExpandForm(values))
                    }
                    is AddressElementNavigator.AutocompleteEvent.OnBack -> {
                        eventListener?.invoke(AutocompleteAddressInteractor.Event.OnValues(values))
                    }
                    null -> Unit
                }

                _collectedAddress.emit(autocompleteAddress)
            }
        }

        // allows merchants to check the box by default and to restore the value later.
        args.config?.address?.isCheckboxSelected?.let {
            _checkboxChecked.value = it
        }
    }

    override fun register(onEvent: (AutocompleteAddressInteractor.Event) -> Unit) {
        eventListener = onEvent
    }

    private fun getCurrentAddress(): AddressDetails {
        val formValues = addressFormController.getCurrentFormValues()

        return AddressDetails(
            name = formValues[IdentifierSpec.Name]?.value,
            address = PaymentSheet.Address(
                city = formValues[IdentifierSpec.City]?.value,
                country = formValues[IdentifierSpec.Country]?.value,
                line1 = formValues[IdentifierSpec.Line1]?.value,
                line2 = formValues[IdentifierSpec.Line2]?.value,
                postalCode = formValues[IdentifierSpec.PostalCode]?.value,
                state = formValues[IdentifierSpec.State]?.value
            ),
            phoneNumber = formValues[IdentifierSpec.Phone]?.value
        )
    }

    fun clickPrimaryButton(
        completedFormValues: Map<IdentifierSpec, FormFieldEntry>?,
        checkboxChecked: Boolean
    ) {
        _formEnabled.value = false
        dismissWithAddress(
            AddressDetails(
                name = completedFormValues?.get(IdentifierSpec.Name)?.value,
                address = PaymentSheet.Address(
                    city = completedFormValues?.get(IdentifierSpec.City)?.value,
                    country = completedFormValues?.get(IdentifierSpec.Country)?.value,
                    line1 = completedFormValues?.get(IdentifierSpec.Line1)?.value,
                    line2 = completedFormValues?.get(IdentifierSpec.Line2)?.value,
                    postalCode = completedFormValues?.get(IdentifierSpec.PostalCode)?.value,
                    state = completedFormValues?.get(IdentifierSpec.State)?.value
                ),
                phoneNumber = completedFormValues?.get(IdentifierSpec.Phone)?.value,
                isCheckboxSelected = checkboxChecked
            )
        )
    }

    @VisibleForTesting
    fun dismissWithAddress(addressDetails: AddressDetails) {
        addressDetails.address?.country?.let { country ->
            eventReporter.onCompleted(
                country = country,
                autocompleteResultSelected = collectedAddress.value?.address?.line1 != null,
                editDistance = addressDetails.editDistance(collectedAddress.value)
            )
        }
        navigator.dismiss(
            AddressLauncherResult.Succeeded(addressDetails)
        )
    }

    fun clickCheckbox(newValue: Boolean) {
        _checkboxChecked.value = newValue
    }

    override fun onAutocomplete(country: String) {
        viewModelScope.launch {
            _collectedAddress.emit(getCurrentAddress())
            navigator.navigateTo(
                AddressElementScreen.Autocomplete(
                    country = country
                )
            )
        }
    }

    internal class Factory(
        private val inputAddressViewModelSubcomponentBuilderProvider:
        Provider<InputAddressViewModelSubcomponent.Builder>
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return inputAddressViewModelSubcomponentBuilderProvider.get()
                .build().inputAddressViewModel as T
        }
    }
}
