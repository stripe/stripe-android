package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.uicore.elements.IdentifierSpec
import com.stripe.android.uicore.forms.FormFieldEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

internal class InputAddressViewModel @Inject constructor(
    val args: AddressElementActivityContract.Args,
    val navigator: AddressElementNavigator,
    private val eventReporter: AddressLauncherEventReporter,
    formControllerProvider: Provider<FormControllerSubcomponent.Builder>
) : ViewModel() {
    private val _collectedAddress = MutableStateFlow(args.config?.address)
    val collectedAddress: StateFlow<AddressDetails?> = _collectedAddress

    private val _formController = MutableStateFlow<FormController?>(null)
    val formController: StateFlow<FormController?> = _formController

    private val _formEnabled = MutableStateFlow(true)
    val formEnabled: StateFlow<Boolean> = _formEnabled

    private val _checkboxChecked = MutableStateFlow(false)
    val checkboxChecked: StateFlow<Boolean> = _checkboxChecked

    init {
        viewModelScope.launch {
            navigator.getResultFlow<AddressDetails?>(AddressDetails.KEY)?.collect {
                val oldAddress = _collectedAddress.value
                val autocompleteAddress = AddressDetails(
                    name = oldAddress?.name ?: it?.name,
                    address = it?.address ?: oldAddress?.address,
                    phoneNumber = oldAddress?.phoneNumber ?: it?.phoneNumber,
                    isCheckboxSelected = oldAddress?.isCheckboxSelected
                        ?: it?.isCheckboxSelected
                )
                _collectedAddress.emit(autocompleteAddress)
            }
        }

        viewModelScope.launch {
            collectedAddress.collect { addressDetails ->
                val initialValues: Map<IdentifierSpec, String?> = addressDetails
                    ?.toIdentifierMap()
                    ?: emptyMap()
                _formController.value = formControllerProvider.get()
                    .viewModelScope(viewModelScope)
                    .stripeIntent(null)
                    .merchantName("")
                    .shippingValues(null)
                    .formSpec(buildFormSpec(addressDetails?.address?.line1 == null))
                    .initialValues(initialValues)
                    .build().formController
            }
        }

        // allows merchants to check the box by default and to restore the value later.
        args.config?.address?.isCheckboxSelected?.let {
            _checkboxChecked.value = it
        }
    }

    private suspend fun getCurrentAddress(): AddressDetails? {
        return formController.value
            ?.formValues
            ?.stateIn(viewModelScope)
            ?.value
            ?.let {
                AddressDetails(
                    name = it[IdentifierSpec.Name]?.value,
                    address = PaymentSheet.Address(
                        city = it[IdentifierSpec.City]?.value,
                        country = it[IdentifierSpec.Country]?.value,
                        line1 = it[IdentifierSpec.Line1]?.value,
                        line2 = it[IdentifierSpec.Line2]?.value,
                        postalCode = it[IdentifierSpec.PostalCode]?.value,
                        state = it[IdentifierSpec.State]?.value
                    ),
                    phoneNumber = it[IdentifierSpec.Phone]?.value
                )
            }
    }

    private fun buildFormSpec(condensedForm: Boolean): LayoutSpec {
        val config = args.config
        val spec = AddressSpecFactory.create(condensedForm, config, ::navigateToAutocompleteScreen)
        return LayoutSpec(listOf(spec))
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

    private fun navigateToAutocompleteScreen() {
        viewModelScope.launch {
            val addressDetails = getCurrentAddress()
            addressDetails?.let {
                _collectedAddress.emit(it)
            }
            addressDetails?.address?.country?.let {
                navigator.navigateTo(
                    AddressElementScreen.Autocomplete(
                        country = it
                    )
                )
            }
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
