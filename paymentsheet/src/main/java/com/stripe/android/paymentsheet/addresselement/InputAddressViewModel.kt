package com.stripe.android.paymentsheet.addresselement

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.addresselement.analytics.AddressLauncherEventReporter
import com.stripe.android.ui.core.injection.NonFallbackInjectable
import com.stripe.android.paymentsheet.injection.InputAddressViewModelSubcomponent
import com.stripe.android.ui.core.FormController
import com.stripe.android.ui.core.elements.AddressSpec
import com.stripe.android.ui.core.elements.AddressType
import com.stripe.android.ui.core.elements.IdentifierSpec
import com.stripe.android.ui.core.elements.LayoutSpec
import com.stripe.android.ui.core.elements.PhoneNumberState
import com.stripe.android.ui.core.injection.FormControllerSubcomponent
import com.stripe.android.ui.core.injection.NonFallbackInjector
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
    private val _collectedAddress = MutableStateFlow(args.config?.defaultValues)
    val collectedAddress: StateFlow<AddressDetails?> = _collectedAddress

    private val _formController = MutableStateFlow<FormController?>(null)
    val formController: StateFlow<FormController?> = _formController

    private val _formEnabled = MutableStateFlow(true)
    val formEnabled: StateFlow<Boolean> = _formEnabled

    init {
        viewModelScope.launch {
            navigator.getResultFlow<AddressDetails?>(AddressDetails.KEY)?.collect {
                val oldShippingAddress = _collectedAddress.value
                val autocompleteAddress = AddressDetails(
                    name = oldShippingAddress?.name ?: it?.name,
                    company = oldShippingAddress?.company ?: it?.company,
                    phoneNumber = oldShippingAddress?.phoneNumber ?: it?.phoneNumber,
                    city = it?.city,
                    country = it?.country,
                    line1 = it?.line1,
                    line2 = it?.line2,
                    state = it?.state,
                    postalCode = it?.postalCode
                )
                _collectedAddress.emit(autocompleteAddress)
            }
        }

        viewModelScope.launch {
            collectedAddress.collect { shippingAddress ->
                val initialValues: Map<IdentifierSpec, String?> = shippingAddress?.let {
                    mapOf(
                        IdentifierSpec.Name to shippingAddress.name,
                        IdentifierSpec.Line1 to shippingAddress.line1,
                        IdentifierSpec.Line2 to shippingAddress.line2,
                        IdentifierSpec.City to shippingAddress.city,
                        IdentifierSpec.State to shippingAddress.state,
                        IdentifierSpec.PostalCode to shippingAddress.postalCode,
                        IdentifierSpec.Country to shippingAddress.country,
                        IdentifierSpec.Phone to shippingAddress.phoneNumber
                    )
                } ?: emptyMap()

                _formController.value = formControllerProvider.get()
                    .viewOnlyFields(emptySet())
                    .viewModelScope(viewModelScope)
                    .stripeIntent(null)
                    .merchantName("")
                    .formSpec(buildFormSpec(shippingAddress?.line1 == null))
                    .initialValues(initialValues)
                    .build().formController
            }
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
                    city = it[IdentifierSpec.City]?.value,
                    country = it[IdentifierSpec.Country]?.value,
                    line1 = it[IdentifierSpec.Line1]?.value,
                    line2 = it[IdentifierSpec.Line2]?.value,
                    postalCode = it[IdentifierSpec.PostalCode]?.value,
                    state = it[IdentifierSpec.State]?.value,
                    phoneNumber = it[IdentifierSpec.Phone]?.value
                )
            }
    }

    private fun buildFormSpec(condensedForm: Boolean): LayoutSpec {
        val phoneNumberState = parsePhoneNumberConfig(args.config?.phone)
        val addressSpec = if (condensedForm) {
            AddressSpec(
                showLabel = false,
                type = AddressType.ShippingCondensed(
                    googleApiKey = args.config?.googlePlacesApiKey,
                    phoneNumberState = phoneNumberState
                ) {
                    viewModelScope.launch {
                        val address = getCurrentAddress()
                        address?.let {
                            _collectedAddress.emit(it)
                        }
                        address?.country?.let {
                            navigator.navigateTo(
                                AddressElementScreen.Autocomplete(
                                    country = it
                                )
                            )
                        }
                    }
                }
            )
        } else {
            AddressSpec(
                showLabel = false,
                type = AddressType.ShippingExpanded(
                    phoneNumberState = phoneNumberState
                )
            )
        }

        val addressSpecWithAllowedCountries = args.config?.allowedCountries?.run {
            addressSpec.copy(allowedCountryCodes = this)
        }

        return LayoutSpec(
            listOf(
                addressSpecWithAllowedCountries ?: addressSpec
            )
        )
    }

    fun clickPrimaryButton() {
        _formEnabled.value = false
        viewModelScope.launch {
            val address = getCurrentAddress()
            address?.let {
                dismissWithAddress(it)
            }
        }
    }

    @VisibleForTesting
    fun dismissWithAddress(address: AddressDetails) {
        address.country?.let { country ->
            eventReporter.onCompleted(
                country = country,
                autocompleteResultSelected = collectedAddress.value?.line1 != null,
                editDistance = address.editDistance(collectedAddress.value)
            )
        }
        navigator.dismiss(
            AddressLauncherResult.Succeeded(address)
        )
    }

    internal class Factory(
        private val injector: NonFallbackInjector
    ) : ViewModelProvider.Factory, NonFallbackInjectable {

        @Inject
        lateinit var subComponentBuilderProvider:
            Provider<InputAddressViewModelSubcomponent.Builder>

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            injector.inject(this)
            return subComponentBuilderProvider.get()
                .build().inputAddressViewModel as T
        }
    }

    internal companion object {
        // This mapping is required to prevent merchants from depending on ui-core
        fun parsePhoneNumberConfig(
            configuration: AddressLauncher.AdditionalFieldsConfiguration?
        ): PhoneNumberState {
            return when (configuration) {
                AddressLauncher.AdditionalFieldsConfiguration.HIDDEN -> PhoneNumberState.HIDDEN
                AddressLauncher.AdditionalFieldsConfiguration.OPTIONAL -> PhoneNumberState.OPTIONAL
                AddressLauncher.AdditionalFieldsConfiguration.REQUIRED -> PhoneNumberState.REQUIRED
                null -> PhoneNumberState.OPTIONAL
            }
        }
    }
}
