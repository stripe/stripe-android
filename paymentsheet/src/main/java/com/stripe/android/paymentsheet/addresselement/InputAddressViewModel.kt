package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.core.model.CountryUtils
import com.stripe.android.elements.Address
import com.stripe.android.elements.BillingDetails
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.uicore.elements.AutocompleteAddressInteractor
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class InputAddressViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    private val eventReporter: AddressLauncherEventReporter,
) : ViewModel(), AutocompleteAddressInteractor {
    private var eventListener: ((AutocompleteAddressInteractor.Event) -> Unit)? = null

    private val initialBillingAddress = args.config?.billingAddress?.toAddressDetails()
    private val initialShippingAddress = args.config?.address

    private val unparsedBillingAddress = initialBillingAddress?.toIdentifierMap()
    private var parsedBillingAddress: Map<IdentifierSpec, String?>? = null

    private val _shippingSameAsBillingState = MutableStateFlow(
        if (canUseShippingSameAsBilling()) {
            ShippingSameAsBillingState.Show(
                isChecked = initialBillingAddress != null && initialShippingAddress == null
            )
        } else {
            ShippingSameAsBillingState.Hide
        }
    )

    private var previousUserInput: Map<IdentifierSpec, String?>? = initialShippingAddress?.toIdentifierMap()
    private var setShippingSameAsShippingAtLeastOnce: Boolean = _shippingSameAsBillingState.value.run {
        this is ShippingSameAsBillingState.Show && isChecked
    }

    val shippingSameAsBillingState = _shippingSameAsBillingState.asStateFlow()

    private val _collectedAddress = MutableStateFlow(value = initialShippingAddress ?: initialBillingAddress)
    val collectedAddress: StateFlow<AddressDetails?> = _collectedAddress

    override val autocompleteConfig: AutocompleteAddressInteractor.Config = AutocompleteAddressInteractor.Config(
        googlePlacesApiKey = args.config?.googlePlacesApiKey,
        autocompleteCountries = args.config?.autocompleteCountries ?: emptySet(),
    )

    val addressFormController = AddressFormController(
        initialValues = _collectedAddress.value?.toIdentifierMap() ?: emptyMap(),
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

        viewModelScope.launch {
            addressFormController.uncompletedFormValues.collectLatest { formValues ->
                val currentBillingSameAsShippingState = _shippingSameAsBillingState.value

                if (currentBillingSameAsShippingState is ShippingSameAsBillingState.Show) {
                    val newValues = formValues.mapValues {
                        it.value.value
                    }

                    /*
                     * This is a trick to grab the parsed billing address details if the user sets billing
                     */
                    if (
                        (setShippingSameAsShippingAtLeastOnce || newValues == unparsedBillingAddress) &&
                        parsedBillingAddress == null
                    ) {
                        parsedBillingAddress = newValues
                    }

                    val sameAsBilling = newValues == parsedBillingAddress

                    if (!sameAsBilling) {
                        previousUserInput = newValues
                    }

                    _shippingSameAsBillingState.value = ShippingSameAsBillingState.Show(
                        isChecked = sameAsBilling
                    )
                }
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
            address = Address(
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
                address = Address(
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

    fun clickBillingSameAsShipping(newValue: Boolean) {
        viewModelScope.launch {
            val currentState = _shippingSameAsBillingState.value

            if (currentState is ShippingSameAsBillingState.Show) {
                val newState = ShippingSameAsBillingState.Show(newValue)

                setShippingSameAsShippingAtLeastOnce = true

                if (newState.isChecked) {
                    (parsedBillingAddress ?: unparsedBillingAddress)?.let {
                        eventListener?.invoke(AutocompleteAddressInteractor.Event.OnValues(it))
                    }
                } else {
                    eventListener?.invoke(
                        AutocompleteAddressInteractor.Event.OnValues(
                            values = previousUserInput ?: emptyMap()
                        )
                    )
                }
            }
        }
    }

    fun clickCheckbox(newValue: Boolean) {
        _checkboxChecked.value = newValue
    }

    private fun canUseShippingSameAsBilling(): Boolean {
        return args.config?.let { config ->
            if (initialBillingAddress == null) {
                return false
            }

            // Country has no default country, can use checkbox for any country
            val country = initialBillingAddress.address?.country ?: return true

            val allowedCountries = config.allowedCountries.takeIf {
                it.isNotEmpty()
            } ?: CountryUtils.supportedBillingCountries

            // Allow if in the allowed countries
            return allowedCountries.contains(country)
        } ?: false
    }

    private fun BillingDetails.toAddressDetails(): AddressDetails {
        return AddressDetails(
            name = name,
            phoneNumber = phone,
            address = address,
        )
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

    sealed interface ShippingSameAsBillingState {
        data object Hide : ShippingSameAsBillingState
        data class Show(
            val isChecked: Boolean,
        ) : ShippingSameAsBillingState
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
